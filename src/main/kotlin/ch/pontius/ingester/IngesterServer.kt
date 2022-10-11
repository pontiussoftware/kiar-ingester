package ch.pontius.ingester

import ch.pontius.ingester.config.Config
import ch.pontius.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.ingester.processors.transformers.ImageTransformer
import ch.pontius.ingester.processors.sources.Sources
import ch.pontius.ingester.processors.sources.XmlFileSource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
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
    }

    /**
     * The [ExecutorService] used to execute continuous jobs, e.g., driven by file watchers.
     */
    private val watcherService: ExecutorService = Executors.newCachedThreadPool {
        val thread = Thread(it, "watcher-thread-${WATCHER_COUNTER.incrementAndGet()}")
        thread.priority = 3
        thread.isDaemon = false
        thread
    }

    /** The [ExecutorService] used to execute ingester Jobs; only one Job can be executed in parallel. */
    private val ingesterService: ExecutorService = Executors.newSingleThreadExecutor {
        val thread = Thread(it, "ingester-job-thread-01")
        thread.priority = 10
        thread.isDaemon = false
        thread
    }

    /** The [ConcurrentUpdateHttp2SolrClient] used to perform updates. */
    private val client: ConcurrentUpdateHttp2SolrClient

    /** Flag indicating that the [IngesterServer] is still running. */
    @Volatile
    var isRunning: Boolean = true
        private set

    init {
        /** Prepare client that performs Apache Solr Updates. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.user != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.user, this.config.password)
        }
        this.client = ConcurrentUpdateHttp2SolrClient.Builder(this.config.server, httpBuilder.build(), true).build()
    }

    /**
     * Schedules the [Job] with the given name.
     *
     * @param jobName The name of the job to schedule.
     */
    fun schedule(jobName: String) {
        val jobConfig = this.config.jobs.find { it.name == jobName } ?: throw IllegalArgumentException("Job configuration with name '$jobName' could not be found.")
        val mappingConfig = this.config.mapper.find { it.name == jobConfig.mappingConfig } ?: throw IllegalArgumentException("Mapping configuration with name '${jobConfig.mappingConfig}' could not be found.")
        val imageConfig = this.config.image.find { it.name == jobConfig.imageConfig } ?: throw IllegalArgumentException("Image configuration with name '${jobConfig.imageConfig}' could not be found.")
        val solrConfig = this.config.ingest.find { it.name == jobConfig.ingestConfig } ?: throw IllegalArgumentException("Image configuration with name '${jobConfig.imageConfig}' could not be found.")

        /* Generate source processor. */
        val source = when (jobConfig.source) {
            Sources.XML -> XmlFileSource(jobConfig.file, mappingConfig)
        }

        /* Configure transformer processor(s). TODO: Make more configurable. */
        val transformer = ImageTransformer(source, imageConfig)

        /* Configure transformer processor(s). TODO: Make more configurable. */
        val solr = ApacheSolrSink(transformer, this.client, solrConfig)

        /* Submit task to ingester service. */
        this.ingesterService.execute {
            runBlocking {
                solr.execute().collect()
            }
        }
    }

    /**
     * Stops this [IngesterServer] and all periodic task registered with it.
     */
    fun stop() {
        if (this.isRunning) {
            this.ingesterService.shutdown()
            this.ingesterService.awaitTermination(5000L, TimeUnit.MILLISECONDS)
            this.watcherService.shutdown()
            this.watcherService.awaitTermination(5000L, TimeUnit.MILLISECONDS)
            this.isRunning = false
        }
    }
}