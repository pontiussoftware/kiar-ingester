package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import ch.pontius.kiar.ingester.solrj.Constants.SYSTEM_FIELDS
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.util.*

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** [FieldValidator] for the different collections. */
    private val validators = HashMap<String,List<FieldValidator>>()

    /** A [Map] of institution names to selected collections. */
    private val institutions = transaction {
        (Institutions innerJoin InstitutionsSolrCollections innerJoin SolrCollections).select(Institutions.name,SolrCollections.name).where {
            (InstitutionsSolrCollections.selected eq true) and (InstitutionsSolrCollections.available eq true)
        }.map {
            it[Institutions.name] to it[SolrCollections.name]
        }.groupBy({ it.first }, { it.second })
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        /* List of collections this [ApacheSolrSink] processes. */
        val allCollections = context.jobTemplate.config?.collections?.filter { it.type == CollectionType.OBJECT } ?: emptyList()
        if (allCollections.isEmpty()) {
            LOGGER.warn("No data collections for ingest found.")
            return this@ApacheSolrSink.input.toFlow(context)
        }

        /* Initializes the document validators. */
        this.initializeValidators(context, allCollections)

        /* Return flow. */
        return this@ApacheSolrSink.input.toFlow(context).onStart {
            this@ApacheSolrSink.prepareIngest(context,allCollections)
        }.onEach { doc ->
            val uuid = doc.get<String>(Field.UUID)
            if (uuid != null) {
                /* Set last change field. */
                if (!doc.has(Field.LASTCHANGE)) {
                    doc.setField(Field.LASTCHANGE, Date())
                }

                /* Ingest into collections. */
                for (collection in allCollections) {
                    try {
                        /* Apply per-institution collection filter. */
                        if (this@ApacheSolrSink.institutions[doc.get<String>(Field.INSTITUTION)]?.contains(collection.name) != true) {
                            LOGGER.debug("Skipping document due to institution not publishing to per-institution filter (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                            continue
                        }

                        /* Apply per-object collection filter. */
                        if (doc.has(Field.PUBLISH_TO)) {
                            doc.getAll<String>(Field.PUBLISH_TO)
                            if (!allCollections.contains(collection)) {
                                LOGGER.debug("Skipping document due to institution not publishing to per-object filter (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                                continue
                            }
                        }

                        /* Apply selector if defined. */
                        if (!collection.selector.isNullOrEmpty()) {
                            val map = doc.fieldNames.associateWith { doc.getFieldValue(it) }
                            try {
                                if (JsonPath.parse(listOf(map)).read<List<*>>("$[?(${collection.selector})])").isEmpty()) {
                                    LOGGER.debug("Skipping document due to selector; no match (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                                    continue
                                }
                            } catch (_: PathNotFoundException) {
                                LOGGER.warn("Skipping document due to selector; path not found (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                                continue
                            }  catch (_: InvalidPathException) {
                                LOGGER.warn("Skipping document due to selector; invalid path (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                                continue
                            }
                        }

                        /* Ingest object. */
                        val validated = this@ApacheSolrSink.validate(collection.name, uuid, doc, context) ?: continue
                        val response = context.solrClient.add(collection.name, validated)
                        if (response.status == 0) {
                            LOGGER.info("Ingested document (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                        } else {
                            LOGGER.error("Failed to ingest document (jobId = ${context.jobId}, docId = $uuid).")
                            context.log(JobLog(null, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.ERROR, "Failed to ingest document due to an Apache Solr error (status = ${response.status})."))
                        }
                    } catch (e: Throwable) {
                        context.log(JobLog(null, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document due to exception: ${e.message}."))
                    }
                }

                /* Increment counter. */
                context.processed()
            } else {
                context.log(JobLog(null, null, null, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document, because UUID is missing."))
            }
        }.onCompletion {
            /* Finalize ingest for all collections. */
            this@ApacheSolrSink.finalizeIngest(context, allCollections)
        }
    }

    /**
     * Initializes the [FieldValidator]s for the different collections.
     *
     * @param context The current [ProcessingContext]
     * @param collections The [List] of available [ApacheSolrCollection]s
     */
    private fun initializeValidators(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
        for (c in collections) {
            /* Prepare HTTP client builder. */
            val copyFields = SchemaRequest.CopyFields().process(context.solrClient, c.name).copyFields.map { it["dest"] }.toSet()
            //TODO (Type-based validation): val types = SchemaRequest.FieldTypes().process(client, c.key).fieldTypes

            /* List of dynamic fixed. */
            val fields = SchemaRequest.Fields().process(context.solrClient, c.name).fields.mapNotNull { f ->
                if (f["name"] !in copyFields && f["name"] !in SYSTEM_FIELDS) {
                    FieldValidator.Regular(f["name"] as String, f["required"] as? Boolean ?: false, f["multiValued"] as? Boolean ?: false, f.contains("default"))
                } else {
                    null
                }
            }

            /* List of dynamic fields. */
            val dynamicFields = SchemaRequest.DynamicFields().process(context.solrClient, c.name).dynamicFields.mapNotNull { f ->
                FieldValidator.Dynamic(f["name"] as String, (f["multiValued"] as? Boolean) ?: false)
            }

            this.validators[c.name] = fields + dynamicFields
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
                val values = validated[validator.name]
                if (values == null || values.valueCount == 0) {
                    context.log(JobLog(null, uuid, collection, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Skipped document, because required field '${validator.name}' is missing."))
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
     * @param context The current [ProcessingContext]
     * @param collections The [List] of available [ApacheSolrCollection]s
     */
    private fun prepareIngest(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
        /* Purge all collections that were configured. */
        for (c in collections) {
            LOGGER.info("Purging collection (name = ${context.jobTemplate.participantName}, collection = $c).")
            val response = context.solrClient.deleteByQuery(c.name, "$FIELD_NAME_PARTICIPANT:${context.jobTemplate.participantName}")
            if (response.status != 0) {
                LOGGER.error("Purge of collection failed (name = ${context.jobTemplate.participantName}, collection = $c). Aborting...")
                throw IllegalArgumentException("Data ingest (name = ${context.jobTemplate.participantName}, collection = $c) failed because delete before import could not be executed.")
            }
            LOGGER.info("Purge of collection successful (name = ${context.jobTemplate.participantName}, collection = $c).")
        }
    }

    /**
     * Finalizes the data ingest operation.
     *
     * @param context The current [ProcessingContext]
     * @param collections The [List] of available [ApacheSolrCollection]s
     */
    private fun finalizeIngest(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
        /* Purge all collections that were configured. */
        for (c in collections) {
            LOGGER.info("Data ingest (name = ${context.jobId}, collection = $c) completed; committing...")
            try {
                val response = context.solrClient.commit(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${context.jobTemplate.participantName}, collection = $c) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${context.jobTemplate.participantName}, collection = $c).")
                }
            } catch (_: SolrServerException) {
                context.solrClient.rollback(c.name)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${context.jobTemplate.participantName}, collection = $c. Rolling back...")
            } catch (_: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${context.jobTemplate.participantName}, collection = $c).")
            }
        }
    }
}