package ch.pontius.kiar.ingester

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobLog
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.watcher.FileWatcher
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.util.findById
import kotlinx.dnq.util.reattach
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
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
    private val activeJobs = ConcurrentHashMap<String,Pair<DbJob,Job>>()

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
        this.store.transactional(true) {
            /* Install file watchers for jobs that should be started automatically. */
            for (template in DbJobTemplate.filter { it.startAutomatically eq true }.asSequence()) {
                this.scheduleWatcher(template.xdId, this.config.ingestPath.resolve(template.participant.name).resolve("${template.name}.${template.type.suffix}"))
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
    fun scheduleJob(jobId: String) {
        val (pipeline, job) = this.store.transactional {
            val job = DbJob.findById(jobId)

            /* Perform sanity check. */
            require(job.status == DbJobStatus.CREATED || job.status == DbJobStatus.HARVESTED) { "Job $jobId cannot be executed because it is in wrong state." }
            job.status = DbJobStatus.SCHEDULED

            /* Return pipeline and job*/
            val pipeline = job.toPipeline(this.config)
            job.status = DbJobStatus.RUNNING
            pipeline to job
        }

        /* Prepare flow including finalization. */
        val context = ProcessingContext(jobId)
        val flow = pipeline.toFlow(context).onStart {
            this@IngesterServer.store.transactional {
                job.reattach()
                job.status = DbJobStatus.RUNNING
            }
        }.onCompletion { e ->
            /* Remove job from list of active jobs. */
            this@IngesterServer.activeJobs.remove(jobId)

            /* Store information about finished job. */
            this@IngesterServer.store.transactional {
                job.reattach()
                when (e) {
                    null -> {
                        LOGGER.info("Data ingest (name = ${job.name}) completed successfully!")
                        job.status = DbJobStatus.INGESTED
                    }
                    is CancellationException -> {
                        job.status = DbJobStatus.ABORTED
                    }
                    else -> {
                        LOGGER.error("Data ingest for job (name = ${job.name}) failed: ${e.message}")
                        job.status = DbJobStatus.FAILED
                    }
                }

                /* Update job with collected metrics. */
                job.processed = context.processed
                job.error = context.error
                job.skipped = context.skipped

                /* Store logs. */
                for (log in context.log) {
                    job.log.add(DbJobLog.new {
                        this.documentId = log.documentId
                        this.context = log.context.toDb()
                        this.level = log.level.toDb()
                        this.description = log.description
                    })
                }
            }
        }.cancellable()

        /* Schedule job for execution. */
        runBlocking {
            this@IngesterServer.activeJobs[jobId] = job to launch(this@IngesterServer.jobDispatcher) {
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
        val job = this.activeJobs[jobId]
        if (job != null) {
            job.second.cancel("The job $jobId has been cancelled by a user.")
            this.store.transactional {
                job.first.reattach()
                if (job.first.status.active) {
                    job.first.status = DbJobStatus.ABORTED
                }
            }
            return true
        }
        return false
    }

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