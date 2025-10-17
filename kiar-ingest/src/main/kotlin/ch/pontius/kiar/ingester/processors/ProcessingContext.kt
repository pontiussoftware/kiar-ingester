package ch.pontius.kiar.ingester.processors

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.config.*
import ch.pontius.kiar.database.config.AttributeMappings.toAttributeMapping
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.config.SolrCollections.toSolrCollection
import ch.pontius.kiar.database.config.Transformers.toTransformerConfig
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Institutions.toInstitution
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.database.jobs.Jobs
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Closeable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A [ProcessingContext] that captures contextual information about a running job.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class ProcessingContext(val jobId: Int, val test: Boolean = false): Closeable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** Number of items processed in this [ProcessingContext]. */
    private val _processed = AtomicLong(0L)

    /** Number of items skipped in this [ProcessingContext]. */
    private val _skipped = AtomicLong(0L)

    /** Number of processing errors this [ProcessingContext]. */
    private val _error = AtomicLong(0L)

    /** Number of processing errors this [ProcessingContext]. */
    private val _aborted = AtomicBoolean(false)

    /** Append-only list of [JobLog] entries. */
    private val buffer: MutableList<JobLog> = Collections.synchronizedList(LinkedList())

    /** The [JobTemplate] backing this [ProcessingContext]. This property is lazily loaded, cached and re-used. */
    val jobTemplate: JobTemplate by lazy {
        transaction {
            val template = (Jobs innerJoin JobTemplates innerJoin SolrConfigs innerJoin EntityMappings)
                .select(JobTemplates.columns + SolrConfigs.columns + EntityMappings.columns)
                .where { Jobs.id eq this@ProcessingContext.jobId }
                .map { it.toJobTemplate() }
                .firstOrNull() ?: throw IllegalStateException("Failed to obtain job template for job with ID ${this@ProcessingContext.jobId}.")

            /* Obtain Apache Solr Configuration with collections. */
            val solr = template.config ?: throw IllegalStateException("Failed to obtain Apache Solr configuration for job with ID ${this@ProcessingContext.jobId}.")
            val collections = SolrCollections.selectAll().where { SolrCollections.solrInstanceId eq solr.id }.map { it.toSolrCollection() }
            val deployments = ImageDeployments.selectAll().where { ImageDeployments.solrInstanceId eq solr.id }.map { it.toImageDeployment() }

            /* Obtain entity mapping. */
            val mapping = template.mapping ?: throw IllegalStateException("Failed to obtain entity mapping configuration for job with ID ${this@ProcessingContext.jobId}.")
            val attributes = AttributeMappings.selectAll().where { AttributeMappings.entityMappingId eq mapping.id }.map { it.toAttributeMapping() }

            /* Obtain transformers. */
            val transformers = Transformers.selectAll().where { Transformers.jobTemplateId eq template.id }.map { it.toTransformerConfig() }

            /* Return copy of template. */
            template.copy(transformers = transformers, config = solr.copy(collections = collections, deployments = deployments), mapping = mapping.copy(attributes = attributes))
        }
    }

    /** The [Http2SolrClient] instance used by this [ProcessingContext]. */
    val solrClient: Http2SolrClient by lazy {
        val config = this.jobTemplate.config ?: throw IllegalStateException("Failed to obtain  Apache Solr configuration configuration for job with ID ${this.jobId}.")

        /* Prepare HTTP client builder. */
        var httpBuilder = Http2SolrClient.Builder(config.server)
        if (config.username != null && config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
        }
        /* Prepare Apache Solr client. */
        httpBuilder.build()
    }

    /** A [Map] of [Institution.name] to [Institution]. */
    val institutions: Map<String, Institution> by lazy {
        transaction {
            (Institutions innerJoin Participants).selectAll().map { it.toInstitution() }.associateBy { it.name }
        }
    }

    /** Number of items processed in this [ProcessingContext]. */
    val processed: Long
        get() = this._processed.get()

    /** Number of items skipped in this [ProcessingContext]. */
    val skipped: Long
        get() = this._skipped.get()

    /** Number of processing errors in this [ProcessingContext]. */
    val error: Long
        get() = this._error.get()

    /** Flag indicating, that this job has been aborted. */
    val aborted: Boolean
        get() = this._aborted.get()

    /**
     * Increments the processed counter.
     */
    fun processed() {
        this._processed.incrementAndGet()
    }

    /**
     * Aborts the job associated with this [ProcessingContext].
     */
    fun abort() = this._aborted.set(true)

    /**
     * Appends a [JobLog] entry to this [ProcessingContext],
     *
     * @param log The [JobLog] to append.
     */
    fun log(log: JobLog) {
        this.buffer.add(log)

        /* Process event. */
        when (log.level) {
            JobLogLevel.WARNING -> LOGGER.info(log.description)
            JobLogLevel.VALIDATION -> {
                LOGGER.warn(log.description)
                this._skipped.incrementAndGet()
            }
            JobLogLevel.ERROR,
            JobLogLevel.SEVERE -> {
                this._error.incrementAndGet()
                LOGGER.error(log.description)
            }
        }

        /* Flush logs. */
        if (this.buffer.size > 100) {
            this.flushLogs()
        }
    }

    /**
     * Flushes all [JobLog]s to the database.
     *
     * Requires an ongoing transaction.
     */
    fun flushLogs() = transaction {
        this@ProcessingContext.buffer.removeIf { log ->
            JobLogs.insert {
                it[JobLogs.jobId] = this.jobId
                it[JobLogs.documentId] = log.documentId?.let { str -> UUID.fromString(str) }
                it[JobLogs.context] = log.context
                it[JobLogs.level] = log.level
                it[JobLogs.description] = log.description
            }
            true
        }
    }

    /**
     * Closes this [ProcessingContext]
     */
    override fun close() {
        /* Close staging database and Apache Solr Client (if necessary). */
        if (!this.test) {
            this.solrClient.close()
        }

        /* Flush remaining logs. */
        this.flushLogs()
    }
}