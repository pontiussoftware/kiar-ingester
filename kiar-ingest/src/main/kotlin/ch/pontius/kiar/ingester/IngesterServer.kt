package ch.pontius.kiar.ingester

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.job.JobId
import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.watcher.FileWatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * The [IngesterServer]. This is the central piece of software that registers.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class IngesterServer(val config: Config) {

    companion object {
        /** Number of watchers. */
        val WATCHER_COUNTER = AtomicLong(0L)

        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeWatchers = ConcurrentHashMap<JobTemplateId,FileWatcher>()

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeJobs = ConcurrentHashMap<JobId, ProcessingContext>()

    /**
     * The [ExecutorService] used to execute continuous jobs, e.g., driven by file watchers.
     */
    private val watcherService: ExecutorService = Executors.newCachedThreadPool {
        val thread = Thread(it, "watcher-thread-${WATCHER_COUNTER.incrementAndGet()}")
        thread.priority = 1
        thread.isDaemon = false
        thread
    }

    /**
     * The [ExecutorService] used to execute continuous jobs, e.g., driven by file watchers.
     */
    private val jobService: ExecutorService = Executors.newSingleThreadExecutor {
        val thread = Thread(it, "ingester-thread")
        thread.priority = 10
        thread.isDaemon = false
        thread
    }

    /**
     * An [ExecutorCoroutineDispatcher] for executing [Job]s.
     */
    private val jobDispatcher = this.jobService.asCoroutineDispatcher()

    /** Flag indicating that the [IngesterServer] is still running. */
    @Volatile
    var isRunning: Boolean = true
        private set

    init {
        transaction {
            /* Install file watchers for jobs that should be started automatically. */
            (JobTemplates innerJoin Participants).selectAll().where { JobTemplates.startAutomatically eq true }.map { it.toJobTemplate() }.forEach {
                this@IngesterServer.scheduleWatcher(it.id!!, this@IngesterServer.config.ingestPath.resolve(it.participantName).resolve("${it.name}.${it.type.suffix}"))
            }

            /* Mark jobs that are still running as interrupted. */
            Jobs.update({ Jobs.status eq JobStatus.RUNNING }) { update ->
                update[status] = JobStatus.INTERRUPTED
                update[modified] = Instant.now()
            }
        }
    }

    /**
     * Schedules the [FileWatcher] for the provided [templateId].
     *
     * @param templateId The [JobTemplateId] of the [FileWatcher] to schedule.
     * @param path The [Path] to the file to watch for.
     * @return True on success, false otherwise.
     */
    fun scheduleWatcher(templateId: JobTemplateId, path: Path): Boolean {
        if (this.activeWatchers.contains(templateId)) return false
        val watcher = FileWatcher(this, templateId, path)
        this.activeWatchers[templateId] = watcher
        this.watcherService.execute(watcher)
        return true
    }

    /**
     * Terminates the [FileWatcher] for the provided [templateId].
     *
     * @param templateId The name of the [FileWatcher] to terminate.
     * @return True on success, false otherwise.
     */
    fun terminateWatcher(templateId: JobTemplateId): Boolean {
        val watcher = this.activeWatchers.remove(templateId) ?: return false
        watcher.cancel()
        return true
    }

    /**
     * Schedules the [Job] with the given name.
     *
     * @param jobId The ID of the job to schedule.
     */
    fun scheduleJob(jobId: JobId, test: Boolean = false) {
        /* Step 1: Perform sanity checks and create pipeline. */
        val (participant, pipeline) = transaction {
            val job = Jobs.getById(jobId)  ?: throw IllegalStateException("Unknown job ID: $jobId.")
            val participant = job.template?.participantName ?: throw IllegalStateException("Job is not associated with a participant.")

            /* Sanity check. */
            require(job.status == JobStatus.FAILED || job.status == JobStatus.HARVESTED || job.status == JobStatus.INTERRUPTED) {
                "Job $jobId cannot be executed because it is in wrong state."
            }

            /* Update job. */
            Jobs.update({ Jobs.id eq jobId }) { update ->
                update[status] = JobStatus.SCHEDULED
                update[modified] = Instant.now()
            }

            /* Return pipeline. */
            participant to job.toPipeline(this@IngesterServer.config, test)
        }

        /* Step 2: Create processing context. */
        val context = ProcessingContext(jobId, participant)
        this.activeJobs[jobId] = context

        /* Step 3: Create flow. */
        val flow = pipeline.toFlow(context).onStart {
            transaction {
                Jobs.update({ Jobs.id eq jobId }) { update ->
                    update[status] = JobStatus.SCHEDULED
                    update[modified] = Instant.now()
                }
            }
        }.onEach {
            /* Flush logs every once in a while. */
            if (context.logSize() >= 5000) {
                context.flushLogs()
            }
        }.takeWhile {
            !context.aborted
        }.onCompletion { e ->
            /* Remove job from list of active jobs. */
            this@IngesterServer.activeJobs.remove(jobId)

            /* Store information about finished job. */
            transaction {
                Jobs.update({ Jobs.id eq jobId }) { update ->
                    if (e != null) {
                        LOGGER.error("Data ingest (ID = $jobId, test = ${test}) failed: ${e.printStackTrace()}")
                        update[status] = JobStatus.FAILED
                    } else if (context.aborted) {
                        LOGGER.warn("Data ingest (ID = $jobId, test = ${test}) was aborted by user!")
                        update[status] = JobStatus.ABORTED
                    } else {
                        LOGGER.info("Data ingest (ID = $jobId, test = ${test}) completed successfully!")
                        if (test) {
                            update[status] = JobStatus.HARVESTED
                        } else {
                            update[status] = JobStatus.INGESTED
                        }
                    }

                    /* Update job with collected metrics. */
                    update[processed] = context.processed
                    update[error] = context.error
                    update[skipped] = context.skipped
                    update[modified] = Instant.now()
                }

                /* Flush logs. */
                context.flushLogs()
            }
        }

        /* Step 4: Schedule job for execution. */
       runBlocking {
            launch(this@IngesterServer.jobDispatcher) {
                flow.collect()
            }
        }
    }


    /**
     * Tries to terminate a running job identified by the given job ID.
     *
     * @param jobId The ID of the job to terminate.
     * @return True on success, false otherwise.
     */
    fun terminateJob(jobId: JobId): Boolean {
        val active = this@IngesterServer.activeJobs[jobId]
        if (active != null) {
            active.abort()
            return true
        }
        return false
    }

    /**
     * Tries to access the [ProcessingContext] of the job identified by the job ID.
     *
     * @param jobId The ID of the job to terminate.
     * @return [ProcessingContext] or null
     */
    fun getContext(jobId: JobId): ProcessingContext? = this.activeJobs[jobId]

    /**
     * Stops this [IngesterServer] and all periodic task registered with it.
     */
    @Synchronized
    fun stop() {
        if (this.isRunning) {
            this.watcherService.shutdown()
            this.watcherService.awaitTermination(10000L, TimeUnit.MILLISECONDS)
            this.isRunning = false
        }
    }
}