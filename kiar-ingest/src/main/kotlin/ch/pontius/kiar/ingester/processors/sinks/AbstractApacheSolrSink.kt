package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import ch.pontius.kiar.ingester.solrj.Constants.SYSTEM_FIELDS
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * A [Sink] that processes [SolrInputDocument]s and and provides basic functions to validate and ingest them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
abstract class AbstractApacheSolrSink(override val input: Source<SolrInputDocument>): Sink<SolrInputDocument> {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(AbstractApacheSolrSink::class.java)!!
    }

    /** [FieldValidator] for the different collections. */
    protected val validators = HashMap<String,List<FieldValidator>>()

    /** A [Map] of institution names to selected collections. */
    protected val institutions by lazy {
        transaction {
            (Institutions innerJoin InstitutionsSolrCollections innerJoin SolrCollections).select(Institutions.name,SolrCollections.name).where {
                (InstitutionsSolrCollections.selected eq true) and (InstitutionsSolrCollections.available eq true)
            }.map {
                it[Institutions.name] to it[SolrCollections.name]
            }.groupBy({ it.first }, { it.second })
        }
    }

    /**
     * Initializes the [FieldValidator]s for the different collections.
     *
     * @param context The current [ProcessingContext]
     * @param collections The [List] of available [ApacheSolrCollection]s
     */
    protected fun initializeValidators(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
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
    protected fun validate(collection: String, uuid: String, doc: SolrInputDocument, context: ProcessingContext): SolrInputDocument? {
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
    protected fun prepareIngest(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
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
    protected fun commit(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
        for (c in collections) {
            LOGGER.info("Data ingest (name = ${context.jobId}, collection = $c) completed; committing...")
            try {
                val response = context.solrClient.commit(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${context.jobTemplate.participantName}, collection = $c) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${context.jobTemplate.participantName}, collection = $c).")
                }
            } catch (e: Throwable) {
                LOGGER.error("Failed to finalize data ingest due to server error (name = ${context.jobTemplate.participantName}, collection = $c. Rolling back...", e)
                context.solrClient.rollback(c.name)
            }
        }
    }

    /**
     * Finalizes the data ingest operation.
     *
     * @param context The current [ProcessingContext]
     * @param collections The [List] of available [ApacheSolrCollection]s
     */
    protected fun abort(context: ProcessingContext, collections: List<ApacheSolrCollection>) {
        for (c in collections) {
            LOGGER.info("Data ingest (name = ${context.jobId}, collection = $c) completed with error; rolling back...")
            try {
                val response = context.solrClient.rollback(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${context.jobTemplate.participantName}, collection = $c) rolled back successfully.")
                } else {
                    LOGGER.warn("Failed to rollback data ingest (name = ${context.jobTemplate.participantName}, collection = $c).")
                }
            } catch (e: Throwable) {
                LOGGER.error("Failed to rollback data ingest due to server error (name = ${context.jobTemplate.participantName}, collection = $c.", e)
            }
        }
    }
}