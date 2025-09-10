package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that generates the [Field.DISPLAY] based on the [Field.OBJECTTYPE].
 *
 * @author Ralph Gasser
 * @author Cristina Illi
 * @version 1.1.1
 */
class
DisplayTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {

    /**
     * Returns a [Flow] of this [ImageDeployment].
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).map { doc ->
        val type = ObjectType.parse(doc.get<String>(Field.OBJECTTYPE) ?: "")
        if (type != null) {
            /* Generate _display_ field. */
            when (type) {
                ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> doc.setField(Field.DISPLAY, listOfNotNull(doc.get<String>(Field.DESIGNATION), doc.get<String>(Field.TITEL)).joinToString(", "))
                ObjectType.FOTOGRAFIE -> doc.setField(Field.DISPLAY, listOfNotNull(doc.asString(Field.DESIGNATION), doc.get<String>(Field.TITEL)).joinToString(", "))
                ObjectType.KUNST -> doc.setField(Field.DISPLAY, listOfNotNull(doc.get<String>(Field.ARTIST), listOfNotNull(doc.get<String>(Field.DESIGNATION), doc.get<String>(Field.TITEL)).joinToString(", ")).joinToString(" - "))
                else -> doc.setField(Field.DISPLAY, doc.get<String>(Field.DESIGNATION) ?: "")
            }

            /* Generate _display_list_ field. */
            val list = when (type) {
                ObjectType.ARCHAEOLOGIE -> listOfNotNull(doc.get<String>(Field.PLACE_FINDING))
                ObjectType.AUDIOVISUELLES_OBJEKT -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.CREATOR))
                ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.AUTHOR), doc.get<String>(Field.PLACE_PUBLICATION))
                ObjectType.BIOLOGIE -> listOfNotNull(doc.get<String>(Field.PLACE_FINDING))
                ObjectType.ETHNOLOGIE ->listOfNotNull(doc.get<String>(Field.CREATOR), doc.get<String>(Field.PLACE_CREATION))
                ObjectType.FOTOGRAFIE -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.PHOTOGRAPHER), doc.get<String>(Field.PLACE_CREATION))
                ObjectType.GEOLOGIE -> listOfNotNull(doc.get<String>(Field.PLACE_FINDING))
                ObjectType.KUNST -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.ARTIST), doc.get<String>(Field.PLACE_CREATION))
            }
            list.forEach { doc.addField(Field.DISPLAY_LIST, it) }
        }

        /* Return document. */
        doc
    }
}