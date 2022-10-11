package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.processors.sources.Source
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument

/**
 * A [Sink] that processes [SolrInputDocument]s and writes them to the log.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class LoggerSink(override val input: Source<SolrInputDocument>): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun execute(): Flow<Unit> {
        /* Generate flow that adds the individual documents. */
        return flow {
            this@LoggerSink.input.toFlow().onCompletion {
                if (it != null) {
                    LOGGER.error("Data ingest failed (test only).")
                }
                LOGGER.info("Data ingest successful (test only).")
            }.collect {
                LOGGER.info(it.toString())
            }
        }
    }
}