package ch.pontius.kiar.ingester

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.job.*
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.watcher.FileWatcher
import ch.pontius.kiar.tasks.PurgeJobLogTask
import ch.pontius.kiar.tasks.RemoveInputFilesTask
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** The [KLogger] instance for [IngesterServer]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * The [IngesterServer]. This is the central piece of software that registers.
 *
 * @author Ralph Gasser
 * @version 2.1.0
 */
class IngesterServer(val config: Config) {

    companion object {
        /** Number of watchers. */
        val WATCHER_COUNTER = AtomicLong(0L)
    }

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeWatchers = ConcurrentHashMap<JobTemplateId,FileWatcher>()

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeJobs = ConcurrentHashMap<JobId, ProcessingContext>()

    /** A [Timer] used to schedule periodic tasks. */
    private val timer = Timer("Task Scheduler")

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
            Jobs.update({ (Jobs.status eq JobStatus.RUNNING) or (Jobs.status eq JobStatus.SCHEDULED)}) { update ->
                update[status] = JobStatus.INTERRUPTED
                update[modified] = Instant.now()
            }
        }

        /* Schedule timer tasks. */
        this.timer.scheduleAtFixedRate(PurgeJobLogTask(this.config), 5000, 86400000)
        this.timer.scheduleAtFixedRate(RemoveInputFilesTask(this.config), 5000, 86400000)
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
     * @param test Whether the Job should only be run as test (i.e., no pushing to Apache Solr)
     */
    fun scheduleJob(jobId: JobId, test: Boolean = false) {
        /* Step 1: Perform sanity checks and create pipeline. */
        val job = transaction {
            val job = Jobs.getById(jobId) ?: throw IllegalStateException("Unknown job ID $jobId.")

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
            job
        }

        /* Step 2: Create processing context. */
        val context = ProcessingContext(jobId, this.config, test)
        this.activeJobs[jobId] = context

        /* Step 3: Create flow. */
        val flow = try {
            job.toPipeline(this@IngesterServer.config, test).toFlow(context).onStart {
                transaction {
                    Jobs.update({ Jobs.id eq jobId }) { update ->
                        update[status] = JobStatus.RUNNING
                        update[modified] = Instant.now()
                    }
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
                            logger.error(e) { "Data ingest (ID = $jobId, test = ${test}) failed." }
                            update[status] = JobStatus.FAILED
                        } else if (context.aborted) {
                            logger.warn { "Data ingest (ID = $jobId, test = ${test}) was aborted by user!" }
                            update[status] = JobStatus.ABORTED
                        } else {
                            logger.info { "Data ingest (ID = $jobId, test = ${test}) completed successfully!" }
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

                    /* Close context. */
                    context.close()
                }
            }
        } catch (e: Throwable) {
            logger.error(e) { "Failed to create job (ID = $jobId, test = ${test}) due to exception." }
            context.log(JobLog(
                jobId = jobId,
                null,
                null,
                JobLogContext.SYSTEM,
                JobLogLevel.SEVERE,
                "Failed to create job (ID = $jobId, test = ${test}) due to exception: ${e.message}."
            ))
            transaction {
                Jobs.update({ Jobs.id eq jobId }) { update ->
                    update[status] = JobStatus.FAILED
                    update[modified] = Instant.now()
                }
                context.close()
            }
            return
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
            this.timer.cancel()
            this.isRunning = false
        }
    }
}