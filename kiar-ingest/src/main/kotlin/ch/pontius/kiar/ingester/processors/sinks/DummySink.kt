package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.apache.solr.common.SolrInputDocument

/**
 * A dummy [Sink] implementation used for debugging.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DummySink(override val input: Source<SolrInputDocument>): Sink<SolrInputDocument> {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).onEach { value ->
        context.processed()
    }
}