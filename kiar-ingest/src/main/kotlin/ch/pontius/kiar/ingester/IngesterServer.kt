package ch.pontius.kiar.ingester

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.watcher.FileWatcher
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.util.findById
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joda.time.DateTime
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * The [IngesterServer]. This is the central piece of software that registers.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class IngesterServer(val store: TransientEntityStore, val config: Config) {

    companion object {
        /** Number of watchers. */
        val WATCHER_COUNTER = AtomicLong(0L)

        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeWatchers = ConcurrentHashMap<String,FileWatcher>()

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val activeJobs = ConcurrentHashMap<String, ProcessingContext>()

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
        this.store.transactional {
            /* Install file watchers for jobs that should be started automatically. */
            for (template in DbJobTemplate.filter { it.startAutomatically eq true }.asSequence()) {
                this.scheduleWatcher(template.xdId, this.config.ingestPath.resolve(template.participant.name).resolve("${template.name}.${template.type.suffix}"))
            }

            /* Mark jobs that are still running as interrupted. */
            for (job in DbJob.filter { it.status eq DbJobStatus.RUNNING }.asSequence()) {
                job.status = DbJobStatus.INTERRUPTED
                job.changedAt = DateTime.now()
            }
        }
    }

    /**
     * Schedules the [FileWatcher] for the provided [templateId].
     *
     * @param templateId The name of the [FileWatcher] to terminate.
     * @param path The [Path] to the file to watch for.
     * @return True on success, false otherwise.
     */
    fun scheduleWatcher(templateId: String, path: Path): Boolean {
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
    fun terminateWatcher(templateId: String): Boolean {
        val watcher = this.activeWatchers.remove(templateId) ?: return false
        watcher.cancel()
        return true
    }

    /**
     * Schedules the [Job] with the given name.
     *
     * @param jobId The ID of the job to schedule.
     */
    fun scheduleJob(jobId: String, test: Boolean = false) {
        /* Step 1: Perform sanity checks and create pipeline. */
        val (participant, pipeline) = this.store.transactional {
            val job = DbJob.findById(jobId)
            val participant = job.template?.participant?.name ?: throw IllegalStateException("Job is not associated with a participant.")

            /* Perform sanity check. */
            require(job.status == DbJobStatus.FAILED || job.status == DbJobStatus.HARVESTED || job.status == DbJobStatus.INTERRUPTED) {
                "Job $jobId cannot be executed because it is in wrong state."
            }
            job.status = DbJobStatus.SCHEDULED
            job.changedAt = DateTime.now()

            /* Return pipeline and job*/
            participant to job.toPipeline(this.config, test)
        }

        /* Step 2: Create processing context. */
        val context = ProcessingContext(jobId, participant)
        this.activeJobs[jobId] = context

        /* Step 3: Create flow. */
        val flow = pipeline.toFlow(context).onStart {
            this@IngesterServer.store.transactional {
                val job = DbJob.findById(jobId)
                job.status = DbJobStatus.RUNNING
            }
        }.takeWhile {
            !context.aborted
        }.onCompletion { e ->
            /* Remove job from list of active jobs. */
            this@IngesterServer.activeJobs.remove(jobId)

            /* Store information about finished job. */
            this@IngesterServer.store.transactional {
                val job = DbJob.findById(jobId)
                if (e != null) {
                    LOGGER.error("Data ingest (name = ${job.name}, test = ${test}) failed: ${e.printStackTrace()}")
                    job.status = DbJobStatus.FAILED
                } else if (context.aborted) {
                    LOGGER.warn("Data ingest (name = ${job.name}, test = ${test}) was aborted by user!")
                    job.status = DbJobStatus.ABORTED
                } else {
                    LOGGER.info("Data ingest (name = ${job.name}, test = ${test}) completed successfully!")
                    if (test) {
                        job.status = DbJobStatus.HARVESTED
                    } else {
                        job.status = DbJobStatus.INGESTED
                    }
                }

                /* Update job with collected metrics. */
                job.processed = context.processed
                job.error = context.error
                job.skipped = context.skipped
                job.changedAt = DateTime.now()

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
    fun terminateJob(jobId: String): Boolean {
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
    fun getContext(jobId: String): ProcessingContext? = this.activeJobs[jobId]

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