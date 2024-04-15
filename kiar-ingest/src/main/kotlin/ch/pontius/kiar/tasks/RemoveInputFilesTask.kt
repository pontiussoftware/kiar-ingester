package ch.pontius.kiar.tasks

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.institution.DbParticipant
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors


/**
 * A simple [TimerTask] used to schedule the removal of old input files.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class RemoveInputFilesTask(private val store: TransientEntityStore, private val config: Config): TimerTask() {
    companion object {
        /** The [Logger] used by this [RemoveInputFilesTask]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    override fun run() {
        var deleted = 0L

        /* Obtain list of participants. */
        val participants = this.store.transactional(true) {
            DbParticipant.all().asSequence().map { it.name }.toList()
        }

        /* Iterate through folders. */
        for (participant in participants) {
            val path = this.config.ingestPath.resolve(participant)
            if (Files.isDirectory(path)) {
                /* Get list of files sorted by last modified date. */
                val files: List<Path> = Files.list(path)
                    .sorted {o1, o2 -> Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2))}
                    .collect(Collectors.toList())

                /* If folder contains to many files, delete oldest. */
                if (files.size > this.config.inputRetentionCount) {
                    for (i in 0 until files.size - this.config.inputRetentionCount) {
                        try {
                            Files.delete(files[i])
                            deleted++
                        } catch (e: IOException) {
                            LOGGER.error("Failed to delete file ${files[i]}.")
                        }
                    }
                }
            }
        }

        /* Log action. */
        if (deleted > 0L) {
            LOGGER.info("Removed $deleted old input files.")
        }
    }
}