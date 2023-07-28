package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that generates the [Field.DISPLAY] based on the [Field.OBJEKTTYP].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DisplayTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {

    /**
     * Returns a [Flow] of this [ImageTransformer].
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).map {
        val type = it.asString(Field.OBJEKTTYP)
        val display = when (ObjectType.parse(type ?: "")) {
            ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> listOf(it.asString(Field.OBJEKTBEZEICHNUNG), it.asString(Field.TITEL), it.asString(Field.AUTOR))
            ObjectType.FOTOGRAFIE -> listOf(it.asString(Field.OBJEKTBEZEICHNUNG), it.asString(Field.TITEL), it.asString(Field.FOTOGRAF))
            ObjectType.KUNST -> listOf(it.asString(Field.OBJEKTBEZEICHNUNG), it.asString(Field.TITEL), it.asString(Field.KUENSTLER))
            else -> listOf(it.asString(Field.OBJEKTBEZEICHNUNG))
        }.filterNotNull().joinToString(", ")
        it.setField(Field.DISPLAY, display)
        it
    }
}