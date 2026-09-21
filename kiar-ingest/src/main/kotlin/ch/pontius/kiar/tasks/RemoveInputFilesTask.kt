package ch.pontius.kiar.tasks

import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.Jobs
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/** The [KLogger] instance for [RemoveInputFilesTask]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A periodic task that removes old job input files (`<jobId>.job`) from the participants' ingest folders, keeping the
 * newest [Config.inputRetentionCount] per participant.
 *
 * Only input files of jobs that can no longer be (re-)started are candidates for deletion. Files that do not follow
 * the `<jobId>.job` naming scheme (files dropped for a watcher, staging files) are never touched.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class RemoveInputFilesTask(private val config: Config): SafeTimerTask() {

    companion object {
        /** Jobs in these states still need (or may need again) their input file. */
        private val PROTECTED_STATES = listOf(JobStatus.CREATED, JobStatus.HARVESTED, JobStatus.SCHEDULED, JobStatus.RUNNING, JobStatus.INTERRUPTED)

        /** Matches job input files. */
        private val JOB_FILE = Regex("^(\\d+)\\.job$")
    }

    override fun execute() {
        var deleted = 0L

        /* Obtain participants and the IDs of jobs whose input must be kept. */
        val (participants, protectedJobs) = transaction {
            val participants = Participants.select(Participants.name).map { it[Participants.name] }
            val protectedJobs = Jobs.select(Jobs.id).where { Jobs.status inList PROTECTED_STATES }.map { it[Jobs.id].value }.toSet()
            participants to protectedJobs
        }

        /* Iterate through folders. */
        for (participant in participants) {
            val path = this.config.ingestPath.resolve(participant)
            if (!Files.isDirectory(path)) continue
            try {
                /* Candidates: job input files of jobs that are not protected, oldest first. */
                val candidates: List<Path> = Files.list(path).use { stream ->
                    stream.asSequence()
                        .filter { Files.isRegularFile(it) }
                        .filter { file ->
                            val jobId = JOB_FILE.matchEntire(file.fileName.toString())?.groupValues?.get(1)?.toIntOrNull()
                            jobId != null && jobId !in protectedJobs
                        }
                        .sortedBy { Files.getLastModifiedTime(it) }
                        .toList()
                }

                /* If folder contains too many files, delete oldest. */
                val surplus = candidates.size - this.config.inputRetentionCount
                for (i in 0 until surplus) {
                    try {
                        Files.delete(candidates[i])
                        deleted++
                    } catch (e: IOException) {
                        logger.error(e) { "Failed to delete file ${candidates[i]}." }
                    }
                }
            } catch (e: Throwable) {
                logger.error(e) { "Failed to clean up input files for participant '$participant' ($path)." }
            }
        }

        /* Log action. */
        if (deleted > 0L) {
            logger.info { "Removed $deleted old input files." }
        }
    }
}
