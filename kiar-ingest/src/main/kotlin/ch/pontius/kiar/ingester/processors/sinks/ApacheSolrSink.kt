package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import ch.pontius.kiar.ingester.solrj.Constants.SYSTEM_FIELDS
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import java.io.Closeable
import java.io.IOException
import java.lang.IllegalStateException

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val config: ApacheSolrConfig): Sink<SolrInputDocument>, Closeable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The [Http2SolrClient] used to interact with Apache Solr.*/
    private val client: Http2SolrClient

    /** [ApacheSolrField] for the different collections. */
    private val validators = HashMap<String,List<ApacheSolrField>>()

    /** */
    private val collections = this.config.collections.filter { it.type == CollectionType.OBJECT }.associate { it.name to it.selector }

    init {
        /* Prepare HTTP client builder. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.username != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.username, this.config.password)
        }
        this.client = httpBuilder.build()

        /* Initializes the document validators. */
        this.initializeValidators()
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<Unit> = flow {
        /* Prepare ingest. */
        this@ApacheSolrSink.prepareIngest(context)

        /* Start collection of incoming flow. */
        this@ApacheSolrSink.input.toFlow(context).collect() { doc ->
            val uuid = doc[Constants.FIELD_NAME_UUID]?.value as? String ?: throw IllegalArgumentException("Field 'uuid' is either missing or has wrong type.")

            /* Add necessary system fields. */
            for (c in this@ApacheSolrSink.config.collections) {
                try {
                    if (this@ApacheSolrSink.isMatch(c.selector, doc)) {
                        if (this@ApacheSolrSink.validate(c.name, uuid, doc, context)) {
                            val response = this@ApacheSolrSink.client.add(c.name, this@ApacheSolrSink.sanitize(c.name, doc))
                            if (response.status == 0) {
                                LOGGER.info("Ingested document (jobId = {}, collection = {}, docId = {}).", context.name, c, uuid)
                                context.processed += 1
                            } else {
                                context.error += 1
                                LOGGER.error("Failed to ingest document (jobId = ${context.name}, docId = $uuid).")
                                context.log.add(JobLog(null, uuid, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to add document due to Apache Solr error."))
                            }
                        } else {
                            context.skipped += 1
                        }
                    }
                } catch (e: SolrServerException) {
                    context.error += 1
                    context.log.add(JobLog(null, uuid, JobLogContext.SYSTEM, JobLogLevel.ERROR, "Failed to ingest document due to Apache Solr error: ${e.message}."))
                }
            }
        }

        /* Finalize ingest for all collections. */
        this@ApacheSolrSink.finalizeIngest(context)

        emit(Unit)
    }


    /**
     * Checks if the provided [SolrInputDocument] matches the provided selector string.
     *
     * @param selector The selector [String]
     * @param document The document to match.
     */
    private fun isMatch(selector: String?, document: SolrInputDocument): Boolean {
        if (selector.isNullOrBlank()) return true
        val terms = selector.trim().split(',').map { it.trim().split(':') }
        return terms.all {
            val match = document[it.getOrNull(0)]?.value == it.getOrNull(1)
            LOGGER.debug("Checking document {}:{} ({}:{}): Match is {}", it[0], it[1], document[it.getOrNull(0)]?.value, it.getOrNull(1), match)
            match
        }
    }

    /**
     * Initializes the [ApacheSolrField]s for the different collections.
     */
    private fun initializeValidators() {
        for (c in this.config.collections) {
            /* Prepare HTTP client builder. */
            val fields = SchemaRequest.Fields().process(this.client, c.name).fields
            val copyFields = SchemaRequest.CopyFields().process(client, c.name).copyFields.map { it["dest"] }.toSet()
            val types = SchemaRequest.FieldTypes().process(client, c.name).fieldTypes /* TODO: Type-based validation. */

            val validators = fields.mapNotNull { schemaField ->
                if (schemaField["name"] !in copyFields && schemaField["name"] !in SYSTEM_FIELDS) {
                    ApacheSolrField(schemaField["name"] as String, schemaField["required"] as Boolean, schemaField["multiValued"] as Boolean, schemaField.contains("default"))
                } else {
                    null
                }
            }
            this.validators[c.name] = validators
        }
    }

    /**
     * Validates the provided [SolrInputDocument]
     *
     * @param collection The name of the collection to validate the [SolrInputDocument] for.
     * @param uuid The UUID of the [SolrInputDocument]
     * @param doc The [SolrInputDocument] to validate.
     * @return True on successful validation, false otherwise.
     */
    private fun validate(collection: String, uuid: String, doc: SolrInputDocument, context: ProcessingContext): Boolean {
        val validators = this.validators[collection] ?: throw IllegalStateException("No validators for collection ${collection}. This is a programmer's error!")
        for (v in validators) {
            if (!v.isValid(doc)) {
                LOGGER.info("Failed to validate document: {} (jobId = {}, collection = {}, docId = {}).", v.isInvalidReason(doc), context.name, collection, uuid)
                context.log.add(JobLog(null, uuid, JobLogContext.METADATA, JobLogLevel.ERROR, "Failed to validate document: ${v.isInvalidReason(doc)}."))
                return false
            }
        }
        return true
    }

    /**
     * Sanitizes the provided [SolrInputDocument] and removes all fields contained in [Constants.INTERNAL_FIELDS].
     *
     * @param doc The [SolrInputDocument] to sanitize.
     * @return Sanitized document (same instance).
     */
    private fun sanitize(collection: String, doc: SolrInputDocument): SolrInputDocument {
        val sanitized = doc.deepCopy()

        /* TODO: Remove fields not needed by this collection. */

        /* Remove fields that have been marked as internal */
        for (f in Constants.INTERNAL_FIELDS) {
            sanitized.removeField(f)
        }
        return sanitized
    }

    /** Purge all collections that were configured. */
    private fun prepareIngest(context: ProcessingContext) {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            if (c.deleteBeforeImport) {
                LOGGER.info("Purging collection (name = ${context.name} collection = ${c.name}).")
                val response = this.client.deleteByQuery(c.name, "$FIELD_NAME_PARTICIPANT:\"${context.name}\"")
                if (response.status != 0) {
                    LOGGER.error("Purge of collection failed (name = ${context.name} collection = ${c.name}). Aborting...")
                    throw IllegalArgumentException("Data ingest (name = ${context.name}, collection = ${c.name}) failed because delete before import could not be executed.")
                }
                LOGGER.info("Purge of collection successful (name = ${context.name} collection = ${c.name}).")
            }
        }
    }

    /* Purge all collections that were configured. */
    private fun finalizeIngest(context: ProcessingContext) {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            LOGGER.info("Data ingest (name = ${context.name}, collection = ${c.name}) completed; committing...")
            try {
                val response = this.client.commit(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
                }
            } catch (e: SolrServerException) {
                this.client.rollback(c.name)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}). Rolling back...")
            } catch (e: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
            }
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}