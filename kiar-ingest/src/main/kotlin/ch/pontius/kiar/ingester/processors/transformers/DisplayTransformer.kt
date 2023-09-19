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
        val type = ObjectType.parse(it.get<String>(Field.OBJEKTTYP) ?: "")

        /* Generate _display_ field. */
        when (type) {
            ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> it.setField(Field.DISPLAY, listOfNotNull(it.get<String>(Field.OBJEKTBEZEICHNUNG), it.get<String>(Field.TITEL), it.get<String>(Field.AUTOR)).joinToString(", "))
            ObjectType.FOTOGRAFIE -> it.setField(Field.DISPLAY, listOfNotNull(it.asString(Field.OBJEKTBEZEICHNUNG), it.get<String>(Field.TITEL), it.get<String>(Field.FOTOGRAF)).joinToString(", "))
            ObjectType.KUNST -> it.setField(Field.DISPLAY, listOfNotNull(it.get<String>(Field.OBJEKTBEZEICHNUNG), it.get<String>(Field.TITEL), it.get<String>(Field.KUENSTLER)).joinToString(", "))
            else -> it.setField(Field.DISPLAY, it.get<String>(Field.OBJEKTBEZEICHNUNG) ?: "")
        }

        /* Generate _display_list_ field. */
        when (type) {
            ObjectType.ARCHAEOLOGIE -> TODO()
            ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> TODO()
            ObjectType.BIOLOGIE -> TODO()
            ObjectType.ETHNOLOGIE -> TODO()
            ObjectType.FOTOGRAFIE -> TODO()
            ObjectType.GEOLOGIE -> TODO()
            ObjectType.KUNST -> TODO()
            null -> TODO()
        }

        /* Return document. */
        it
    }
}