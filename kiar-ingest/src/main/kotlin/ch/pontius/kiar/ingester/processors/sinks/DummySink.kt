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
import kotlinx.coroutines.flow.onEach
import org.apache.solr.common.SolrInputDocument
import java.util.*

/** The [KLogger] instance for [DummySink]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A dummy [Sink] implementation used for debugging.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class DummySink(input: Source<SolrInputDocument>): AbstractApacheSolrSink(input) {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        /* List of collections this [ApacheSolrSink] processes. */
        val collections = context.jobTemplate.config?.collections?.filter { it.type == CollectionType.OBJECT } ?: emptyList()
        if (collections.isEmpty()) {
            logger.warn { "No data collections for ingest found." }
            return this@DummySink.input.toFlow(context)
        }

        /* Initializes the document validators. */
        this.initializeValidators(context, collections)

        /* Return flow. */
        return this@DummySink.input.toFlow(context).onEach { doc ->
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
                        if (this@DummySink.institutions[doc.get<String>(Field.INSTITUTION)]?.contains(collection.name) != true) {
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
                                logger.warn { "Skipping document due to selector; path not found (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                                continue
                            }  catch (_: InvalidPathException) {
                                logger.warn { "Skipping document due to selector; invalid path (jobId = ${context.jobId}, collection = $collection, docId = $uuid)." }
                                continue
                            }
                        }

                        /* Validate and stage object. */
                        this@DummySink.validate(collection.name, uuid, doc, context)
                    } catch (e: Throwable) {
                        context.log(JobLog(null, uuid, collection.name, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document due to exception: ${e.message}."))
                    }
                }

                /* Increment counter. */
                context.processed()
            } else {
                context.log(JobLog(null, null, null, JobLogContext.SYSTEM, JobLogLevel.SEVERE, "Failed to ingest document, because UUID is missing."))
            }
        }
    }
}