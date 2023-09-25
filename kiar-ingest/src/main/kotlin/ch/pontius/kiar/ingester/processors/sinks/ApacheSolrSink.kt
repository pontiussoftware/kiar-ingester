package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import ch.pontius.kiar.ingester.solrj.Constants.SYSTEM_FIELDS
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.lang.IllegalStateException

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val config: ApacheSolrConfig): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** [FieldValidator] for the different collections. */
    private val validators = HashMap<String,List<FieldValidator>>()

    /** List of collections this [ApacheSolrSink] processes. */
    private val collections = this.config.collections.filter { it.type == CollectionType.OBJECT }.associate { it.name to it.selector }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<Unit> {
        /* Prepare HTTP client builder. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.username != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.username, this.config.password)
        }
        /* Prepare Apache Solr client. */
        val client = httpBuilder.build()

        /* Initializes the document validators. */
        this.initializeValidators(client)

        /* Return flow. */
        return flow {
            /* Prepare ingest. */
            this@ApacheSolrSink.prepareIngest(client, context)

            /* Start collection of incoming flow. */
            this@ApacheSolrSink.input.toFlow(context).collect() { doc ->
                val uuid = doc.get<String>(Field.UUID)
                if (uuid != null) {
                    for ((collection, selector) in this@ApacheSolrSink.collections) {
                        try {
                            if (this@ApacheSolrSink.isMatch(selector, doc)) {
                                val validated = this@ApacheSolrSink.validate(collection, uuid, doc, context) ?: continue
                                val response = client.add(collection, validated)
                                if (response.status == 0) {
                                    LOGGER.info("Ingested document (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                                    context.processed += 1
                                } else {
                                    context.error += 1
                                    LOGGER.error("Failed to ingest document (jobId = ${context.jobId}, docId = $uuid).")
                                    context.log.add(JobLog(null, uuid, collection, JobLogContext.SYSTEM, JobLogLevel.ERROR, "Failed to ingest document due to an Apache Solr error (status = ${response.status})."))
                                }
                            } else {
                                context.skipped += 1
                            }
                        } catch (e: Throwable) {
                            context.error += 1
                            context.log.add(JobLog(null, uuid, collection, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document due to exception: ${e.message}."))
                        }
                    }
                } else {
                    context.log.add(JobLog(null, "<undefined>", null, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document, because UUID is missing."))
                    context.skipped += 1
                }
            }

            /* Finalize ingest for all collections. */
            this@ApacheSolrSink.finalizeIngest(client, context)

            emit(Unit)
        }.onCompletion {
            client.close()
        }
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
            document[it.getOrNull(0)]?.value == it.getOrNull(1)
        }
    }

    /**
     * Initializes the [FieldValidator]s for the different collections.
     */
    private fun initializeValidators(client: Http2SolrClient) {
        for (c in this.collections) {
            /* Prepare HTTP client builder. */
            val copyFields = SchemaRequest.CopyFields().process(client, c.key).copyFields.map { it["dest"] }.toSet()
            val types = SchemaRequest.FieldTypes().process(client, c.key).fieldTypes /* TODO: Type-based validation. */

            /* List of dynamic fixed. */
            val fields = SchemaRequest.Fields().process(client, c.key).fields.mapNotNull { f ->
                if (f["name"] !in copyFields && f["name"] !in SYSTEM_FIELDS) {
                    FieldValidator.Regular(f["name"] as String, f["required"] as? Boolean ?: false, f["multiValued"] as? Boolean ?: false, f.contains("default"))
                } else {
                    null
                }
            }

            /* List of dynamic fields. */
            val dynamicFields = SchemaRequest.DynamicFields().process(client, c.key).dynamicFields.mapNotNull { f ->
                FieldValidator.Dynamic(f["name"] as String, (f["multiValued"] as? Boolean) ?: false)
            }

            this.validators[c.key] = fields + dynamicFields
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
    private fun validate(collection: String, uuid: String, doc: SolrInputDocument, context: ProcessingContext): SolrInputDocument? {
        /* Validated document (empty at first). */
        val validated = SolrInputDocument()

        /* Obtain validator for collection. */
        val validators = this.validators[collection] ?: throw IllegalStateException("No validators for collection ${collection}. This is a programmer's error!")

        /* Now validate all present fields and transfer them, based on the validation outcome. */
        for ((name, field) in doc.entries) {
            /* Find validator for field. If it is not contained in schema, skip the field. */
            val validator = validators.firstOrNull { it.isMatch(name) } ?: continue

            /* Validate field using the validator. */
            if (validator.isValid(field)) {
                validated[name] = field
            } else if (validator.required && !validator.hasDefault) { /* Required field is invalid; skip document. */
                context.log.add(JobLog(null, uuid, collection, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Skipped document, because required field '${name}' failed validation: ${validator.getReason(field)}"))
                return null
            } else { /* Optional field is invalid; skip field. */
                context.log.add(JobLog(null, uuid, collection, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Truncated document, because field '${name}' failed validation: ${validator.getReason(field)}"))
            }
        }

        /* Now make sure that all required fields that don't have a default value, are accounted for. */
        for (validator in validators) {
            if (validator.required && !validator.hasDefault && validator is FieldValidator.Regular) {
                val field = validated[validator.name]
                if (field == null || field.valueCount == 0) {
                    return null /* Required field is missing. */
                }
            }
        }

        /* Return validated document. */
        return validated
    }

    /**
     * Purge all collections that were configured.
     *
     * @param client The [Http2SolrClient] to perform the operation with.
     * @param context The current [ProcessingContext]
     */
    private fun prepareIngest(client: Http2SolrClient, context: ProcessingContext) {
        /* Purge all collections that were configured. */
        for (c in this.collections) {
            LOGGER.info("Purging collection (name = ${context.jobId} collection = ${c.key}).")
            val response = client.deleteByQuery(c.key, "$FIELD_NAME_PARTICIPANT:\"${context.jobId}\"")
            if (response.status != 0) {
                LOGGER.error("Purge of collection failed (name = ${context.jobId} collection = ${c.key}). Aborting...")
                throw IllegalArgumentException("Data ingest (name = ${context.jobId}, collection = ${c.key}) failed because delete before import could not be executed.")
            }
            LOGGER.info("Purge of collection successful (name = ${context.jobId} collection = ${c.key}).")
        }
    }

    /**
     * Finalizes the data ingest operation.
     *
     * @param client The [Http2SolrClient] to perform the operation with.
     * @param context The current [ProcessingContext]
     */
    private fun finalizeIngest(client: Http2SolrClient, context: ProcessingContext) {
        /* Purge all collections that were configured. */
        for (c in this.collections) {
            LOGGER.info("Data ingest (name = ${context.jobId}, collection = ${c.key}) completed; committing...")
            try {
                val response = client.commit(c.key)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.key}) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.key}).")
                }
            } catch (e: SolrServerException) {
                client.rollback(c.key)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.key}). Rolling back...")
            } catch (e: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.key}).")
            }
        }
    }
}