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
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.apache.solr.common.SolrInputDocument
import java.util.*

/** The [KLogger] instance for [ApacheSolrSink]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.4.1
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
            logger.warn { "No data collections for ingest found." }
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
                            logger.debug { "Skipping document due to institution not publishing to per-institution filter (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                            continue
                        }

                        /* Apply per-object collection filter. */
                        if (doc.has(Field.PUBLISH_TO)) {
                            doc.getAll<String>(Field.PUBLISH_TO)
                            if (!collections.contains(collection)) {
                                logger.debug { "Skipping document due to institution not publishing to per-object filter (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                                continue
                            }
                        }

                        /* Apply selector if defined. */
                        if (!collection.selector.isNullOrEmpty()) {
                            val map = doc.fieldNames.associateWith { doc.getFieldValue(it) }
                            try {
                                if (JsonPath.parse(listOf(map)).read<List<*>>("$[?(${collection.selector})])").isEmpty()) {
                                    logger.debug { "Skipping document due to selector; no match (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                                    continue
                                }
                            } catch (_: PathNotFoundException) {
                                context.log(JobLog(context.jobId, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.WARNING, "Skipping document due to selector; path not found."))
                                continue
                            }  catch (_: InvalidPathException) {
                                context.log(JobLog(context.jobId, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.WARNING, "Skipping document due to selector; invalid path."))
                                continue
                            }
                        }

                        /* Validate and stage object. */
                        val validated = this@ApacheSolrSink.validate(collection.name, uuid, doc, context) ?: continue
                        val response = context.solrClient.add(collection.name, validated)
                        if (response.status == 0) {
                            logger.debug { "Ingested document (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                        } else {
                            context.log(JobLog(context.jobId, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.ERROR, "Failed to ingest document due to an Apache Solr error (status = ${response.status})."))
                        }
                    } catch (e: Throwable) {
                        context.log(JobLog(context.jobId, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document due to exception: ${e.message}."))
                    }
                }

                /* Increment counter. */
                context.processed()
            } else {
                context.log(JobLog(context.jobId, null, null, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document, because UUID is missing."))
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