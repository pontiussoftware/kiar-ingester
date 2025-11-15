package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.apache.solr.common.SolrInputDocument
import java.util.*

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
class ApacheSolrSink(input: Source<SolrInputDocument>): AbstractApacheSolrSink(input) {
    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        /* List of collections this [ApacheSolrSink] processes. */
        val collections = context.jobTemplate.config?.collections?.filter { it.type == CollectionType.OBJECT } ?: emptyList()
        if (collections.isEmpty()) {
            LOGGER.warn("No data collections for ingest found.")
            return this@ApacheSolrSink.input.toFlow(context)
        }

        /* Initializes the document validators. */
        this.initializeValidators(context, collections)

        /* Return flow. */
        return this@ApacheSolrSink.input.toFlow(context).onStart {
            this@ApacheSolrSink.prepareIngest(context, collections)
        }.onEach { doc ->
            val uuid = doc.get<String>(Field.UUID)
            if (uuid != null) {
                /* Set last change field. */
                if (!doc.has(Field.LASTCHANGE)) {
                    doc.setField(Field.LASTCHANGE, Date())
                }

                /* Ingest into collections. */
                for (collection in collections) {
                    try {
                        /* Apply per-institution collection filter. */
                        if (this@ApacheSolrSink.institutions[doc.get<String>(Field.INSTITUTION)]?.contains(collection.name) != true) {
                            LOGGER.debug("Skipping document due to institution not publishing to per-institution filter (jobId = {}, collection = {}, docId = {}).", context.jobId, collection, uuid)
                            continue
                        }

                        /* Apply per-object collection filter. */
                        if (doc.has(Field.PUBLISH_TO)) {
                            doc.getAll<String>(Field.PUBLISH_TO)
                            if (!collections.contains(collection)) {
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

                        /* Validate and stage object. */
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
        }.onCompletion { e ->
            /* Finalize ingest for all collections. */
            if (e != null) {
                this@ApacheSolrSink.abort(context, collections)
            } else {
                this@ApacheSolrSink.commit(context, collections)
            }
        }
    }
}