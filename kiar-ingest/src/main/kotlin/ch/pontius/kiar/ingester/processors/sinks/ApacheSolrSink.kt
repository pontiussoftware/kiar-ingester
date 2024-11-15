package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import ch.pontius.kiar.ingester.solrj.Constants.SYSTEM_FIELDS
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.getAll
import ch.pontius.kiar.ingester.solrj.has
import ch.pontius.kiar.ingester.solrj.setField
import kotlinx.coroutines.flow.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.util.Date
import java.util.UUID

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.2.1
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val config: ApacheSolrConfig): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** [FieldValidator] for the different collections. */
    private val validators = HashMap<String,List<FieldValidator>>()

    /** List of collections this [ApacheSolrSink] processes. */
    private val collections = this.config.collections.filter { it.type == CollectionType.OBJECT }.map { it.name }

    /** A [Map] of [DbInstitution] name to selected collections. */
    private val institutions = DbInstitution.filter { (it.selectedCollections.isNotEmpty() ) }.asSequence().associate {
        it.name to it.selectedCollections.asSequence().map { c -> c.name }.toSet()
    }

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
                    /* Set last change field. */
                    if (!doc.has(Field.LASTCHANGE)) {
                        doc.setField(Field.LASTCHANGE, Date())
                    }

                    /* Ingest into collections. */
                    for (collection in this@ApacheSolrSink.collections) {
                        try {
                            /* Apply per-institution collection filter. */
                            if (this@ApacheSolrSink.institutions[doc.get<String>(Field.INSTITUTION)]?.contains(collection) != true) continue

                            /* Apply per-object collection filter. */
                            if (doc.has(Field.PUBLISH_TO)) {
                                val collections = doc.getAll<String>(Field.PUBLISH_TO)
                                if (!collections.contains(collection)) {
                                    continue
                                }
                            }

                            /* Ingest object. */
                            val validated = this@ApacheSolrSink.validate(collection, uuid, doc, context) ?: continue
                            val response = client.add(collection, validated)
                            if (response.status == 0) {
                                LOGGER.info("Ingested document (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                            } else {
                                LOGGER.error("Failed to ingest document (jobId = ${context.jobId}, docId = $uuid).")
                                context.log(JobLog(null, uuid, collection, JobLogContext.SYSTEM, JobLogLevel.ERROR, "Failed to ingest document due to an Apache Solr error (status = ${response.status})."))
                            }
                        } catch (e: Throwable) {
                            context.log(JobLog(null, uuid, collection, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document due to exception: ${e.message}."))
                        }
                    }

                    /* Increment counter. */
                    context.processed()
                } else {
                    context.log(JobLog(null, null, null, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document, because UUID is missing."))
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
     * Initializes the [FieldValidator]s for the different collections.
     */
    private fun initializeValidators(client: Http2SolrClient) {
        for (c in this.collections) {
            /* Prepare HTTP client builder. */
            val copyFields = SchemaRequest.CopyFields().process(client, c).copyFields.map { it["dest"] }.toSet()
            //TODO (Type-based validation): val types = SchemaRequest.FieldTypes().process(client, c.key).fieldTypes

            /* List of dynamic fixed. */
            val fields = SchemaRequest.Fields().process(client, c).fields.mapNotNull { f ->
                if (f["name"] !in copyFields && f["name"] !in SYSTEM_FIELDS) {
                    FieldValidator.Regular(f["name"] as String, f["required"] as? Boolean ?: false, f["multiValued"] as? Boolean ?: false, f.contains("default"))
                } else {
                    null
                }
            }

            /* List of dynamic fields. */
            val dynamicFields = SchemaRequest.DynamicFields().process(client, c).dynamicFields.mapNotNull { f ->
                FieldValidator.Dynamic(f["name"] as String, (f["multiValued"] as? Boolean) ?: false)
            }

            this.validators[c] = fields + dynamicFields
        }
    }

    /**
     * Validates the provided [SolrInputDocument]
     *
     * @param collection The name of the collection to validate the [SolrInputDocument] for.
     * @param uuid The [UUID] of the [SolrInputDocument] as [String]
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
                context.log(JobLog(null, uuid, collection, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Skipped document, because required field '${name}' failed validation: ${validator.getReason(field)}"))
                return null
            } else { /* Optional field is invalid; skip field. */
                context.log(JobLog(null, uuid, collection, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Truncated document, because field '${name}' failed validation: ${validator.getReason(field)}"))
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
            LOGGER.info("Purging collection (name = ${context.participant}, collection = $c).")
            val response = client.deleteByQuery(c, "$FIELD_NAME_PARTICIPANT:${context.participant}")
            if (response.status != 0) {
                LOGGER.error("Purge of collection failed (name = ${context.participant}, collection = $c). Aborting...")
                throw IllegalArgumentException("Data ingest (name = ${context.participant}, collection = $c) failed because delete before import could not be executed.")
            }
            LOGGER.info("Purge of collection successful (name = ${context.participant}, collection = $c).")
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
            LOGGER.info("Data ingest (name = ${context.jobId}, collection = $c) completed; committing...")
            try {
                val response = client.commit(c)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.config.name}, collection = $c) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${this@ApacheSolrSink.config.name}, collection = $c).")
                }
            } catch (e: SolrServerException) {
                client.rollback(c)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${this@ApacheSolrSink.config.name}, collection = $c. Rolling back...")
            } catch (e: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${this@ApacheSolrSink.config.name}, collection = $c).")
            }
        }
    }
}