package ch.pontius.ingester

import ch.pontius.ingester.config.Config
import ch.pontius.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.ingester.processors.sinks.LoggerSink
import ch.pontius.ingester.processors.sinks.Sinks
import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.processors.sources.Sources
import ch.pontius.ingester.processors.sources.XmlFileSource
import ch.pontius.ingester.watcher.FileWatcher
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.solr.common.SolrInputDocument
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
class IngesterServer(val config: Config) {

    companion object {
        /** Number of watchers. */
        val WATCHER_COUNTER = AtomicLong(0L)

        /** The [Logger] used by this [IngesterServer]. */
        private val LOGGER: Logger = LogManager.getLogger()
    }

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
        for (job in this.config.jobs) {
            if (job.startOnCreation) {
                this.service.execute(FileWatcher(this, job))
            }
        }
    }

    /**
     * Schedules the [Job] with the given name.
     *
     * @param jobName The name of the job to schedule.
     */
    fun schedule(jobName: String) = this.service.execute {
        this.execute(jobName)
    }

    /**
     * Executes the [Job] with the given name.
     *
     * @param jobName The name of the job to schedule.
     */
    fun execute(jobName: String) {
        val jobConfig = this.config.jobs.find { it.name == jobName } ?: throw IllegalArgumentException("Job configuration with name '$jobName' could not be found.")
        val mappingConfig = this.config.mappers.find { it.name == jobConfig.mappingConfig } ?: throw IllegalArgumentException("Mapping configuration with name '${jobConfig.mappingConfig}' could not be found.")
        val solrConfig = this.config.solr.find { it.name == jobConfig.solrConfig } ?: throw IllegalArgumentException("Apache Solr configuration with name '${jobConfig.solrConfig}' could not be found.")

        /* Generate source processor. */
        var source: Source<SolrInputDocument> = when (jobConfig.source) {
            Sources.XML -> XmlFileSource(jobConfig.name, jobConfig.file, mappingConfig)
        }

        /* Generate and append transformer processors. */
        for (t in jobConfig.transformers) {
            source = t.type.newInstance(source, t.parameters)
        }

        /* Generate sink processor(s). */
        val sink = when (jobConfig.sink) {
            Sinks.SOLR -> ApacheSolrSink(source, solrConfig)
            Sinks.LOGGER -> LoggerSink(source)
        }

        /* Execute the pipeline. */
        try {
            sink.execute()
            LOGGER.info("Data ingest (name = ${jobConfig.name}) completed successfully!")
        } catch (e: Throwable) {
            LOGGER.error("Data ingest (name = ${jobConfig.name}) failed: ${e.message}")
        }
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