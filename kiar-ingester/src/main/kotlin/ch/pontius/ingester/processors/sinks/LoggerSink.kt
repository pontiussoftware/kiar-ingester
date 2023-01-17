package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.processors.sources.Source
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
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
     * Executes this [LoggerSink] and the associated processing pipeline.
     */
    override fun execute() = runBlocking {
        this@LoggerSink.input.toFlow().collect {
            LOGGER.info(it.toString())
        }
    }
}