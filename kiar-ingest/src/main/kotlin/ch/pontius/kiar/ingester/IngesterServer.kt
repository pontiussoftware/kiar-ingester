package ch.pontius.kiar.ingester

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.database.job.DbJob
import ch.pontius.kiar.database.job.DbJobLog
import ch.pontius.kiar.database.job.DbJobSource
import ch.pontius.kiar.database.job.DbJobStatus
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.watcher.FileWatcher
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.util.reattach
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joda.time.DateTime
import java.nio.file.Path
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
class IngesterServer(private val store: TransientEntityStore, val config: Config) {

    companion object {
        /** Number of watchers. */
        val WATCHER_COUNTER = AtomicLong(0L)

        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

    /** A [Map] of [FileWatcher]s that are currently running. */
    private val watchers = HashMap<String,FileWatcher>()

    /**
     * The [ExecutorService] used to execute continuous jobs, e.g., driven by file watchers.
     */
    private val service: ExecutorService = Executors.newCachedThreadPool {
        val thread = Thread(it, "ingester-thread-${WATCHER_COUNTER.incrementAndGet()}")
        thread.priority = 10
        thread.isDaemon = false
        thread
    }

    /** Flag indicating that the [IngesterServer] is still running. */
    @Volatile
    var isRunning: Boolean = true
        private set

    init {
        this.store.transactional(true) {
            /* Install file watchers for jobs that should be started automatically. */
            for (template in DbJobTemplate.filter { it.startAutomatically eq true }.asSequence()) {
                this.scheduleWatcher(template.name, template.sourcePath(this.config))
            }
        }
    }

    /**
     * Schedules the [FileWatcher] for the provided [name].
     *
     * @param name The name of the [FileWatcher] to terminate.
     * @param path The [Path] to the file to watch for.
     * @return True on success, false otherwise.
     */
    fun scheduleWatcher(name: String, path: Path): Boolean {
        if (this.watchers.contains(name)) return false
        val watcher = FileWatcher(this, name, path)
        this.watchers[name] = watcher
        this.service.execute(watcher)
        return true
    }

    /**
     * Terminates the [FileWatcher] for the provided [name].
     *
     * @param name The name of the [FileWatcher] to terminate.
     * @return True on success, false otherwise.
     */
    fun terminateWatcher(name: String): Boolean {
        val watcher = this.watchers.remove(name) ?: return false
        watcher.cancel()
        return true
    }

    /**
     * Schedules the [Job] with the given name.
     *
     * @param jobName The name of the job to schedule.
     */
    fun schedule(jobName: String, username: String = "SYSTEM") = this.service.execute {
        this.execute(jobName, username)
    }

    /**
     * Executes the [Job] with the given name.
     *
     * @param templateName The name of the [DbJobTemplate] to schedule.
     * @param username The name of the user who scheduled the [DbJobTemplate].
     */
    fun execute(templateName: String, username: String = "SYSTEM") {
        /* Start transaction. */
        val (job, pipeline) = this.store.transactional {
            /* Load template. */
            val template = DbJobTemplate.filter { (it.name eq templateName) }.firstOrNull()
                ?: throw IllegalArgumentException("Could not find template with name $templateName.")

            /* Create job object. */
            val pipeline = template.newInstance(this.config)

            /* Create new job entry in database. */
            val job = DbJob.new {
                this.name = template.name + "${System.currentTimeMillis()}"
                this.template = template
                this.source = DbJobSource.WATCHER
                this.status = DbJobStatus.RUNNING
                this.createdAt = DateTime.now()
                this.createdBy = DbUser.filter { it.name eq username }.firstOrNull()
                this.createdByName = username
            }
            job to pipeline
        }


        /* Prepare flow including finalization. */
        val context = ProcessingContext(templateName)
        val flow = pipeline.toFlow(context).onCompletion { e ->
            this@IngesterServer.store.transactional {
                job.reattach(it)
                if (e != null) {
                    LOGGER.error("Data ingest for job (name = ${job.name}) failed: ${e.message}")
                    job.status = DbJobStatus.INGESTED
                } else {
                    LOGGER.info("Data ingest (name = ${job.name}) completed successfully!")
                    job.status = DbJobStatus.FAILED
                }

                /* Update job with collected metrics. */
                job.processed = context.processed
                job.error = context.error
                job.skipped = context.skipped

                for (log in context.log) {
                    job.log.add(DbJobLog.new {
                        this.documentId = log.documentId
                        this.context = log.context.toDb()
                        this.level = log.level.toDb()
                        this.description = log.description
                    })
                }
            }
        }

        /* Execute job. */
        runBlocking { flow.collect() }
    }

    /**
     * Stops this [IngesterServer] and all periodic task registered with it.
     */
    fun stop() {
        if (this.isRunning) {
            this.service.shutdown()
            this.service.awaitTermination(10000L, TimeUnit.MILLISECONDS)
            this.isRunning = false
        }
    }
}