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
 * @author Cristina Illi
 * @version 1.1.0
 */
class DisplayTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {

    /**
     * Returns a [Flow] of this [ImageDeployment].
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).map { doc ->
        val type = ObjectType.parse(doc.get<String>(Field.OBJEKTTYP) ?: "")
        if (type != null) {
            /* Generate _display_ field. */
            when (type) {
                ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> doc.setField(Field.DISPLAY, listOfNotNull(doc.get<String>(Field.OBJEKTBEZEICHNUNG), doc.get<String>(Field.TITEL)).joinToString(", "))
                ObjectType.FOTOGRAFIE -> doc.setField(Field.DISPLAY, listOfNotNull(doc.asString(Field.OBJEKTBEZEICHNUNG), doc.get<String>(Field.TITEL)).joinToString(", "))
                ObjectType.KUNST -> doc.setField(Field.DISPLAY, listOfNotNull(doc.get<String>(Field.OBJEKTBEZEICHNUNG), doc.get<String>(Field.TITEL), doc.get<String>(Field.KUENSTLER)).joinToString(", "))
                else -> doc.setField(Field.DISPLAY, doc.get<String>(Field.OBJEKTBEZEICHNUNG) ?: "")
            }

            /* Generate _display_list_ field. */
            val list = when (type) {
                ObjectType.ARCHAEOLOGIE -> listOfNotNull(doc.get<String>(Field.POLITISCHER_FUNDORT))
                ObjectType.BIBLIOGRAPHISCHES_OBJEKT -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.AUTOR), doc.get<String>(Field.ERSCHEINUNGSORT))
                ObjectType.BIOLOGIE -> listOfNotNull(doc.get<String>(Field.POLITISCHER_FUNDORT))
                ObjectType.ETHNOLOGIE ->listOfNotNull(doc.get<String>(Field.HERSTELLER), doc.get<String>(Field.HERSTELLUNGSORT))
                ObjectType.FOTOGRAFIE -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.FOTOGRAF), doc.get<String>(Field.HERSTELLUNGSORT))
                ObjectType.GEOLOGIE -> listOfNotNull(doc.get<String>(Field.POLITISCHER_FUNDORT))
                ObjectType.KUNST -> listOfNotNull(doc.get<String>(Field.TITEL), doc.get<String>(Field.KUENSTLER), doc.get<String>(Field.HERSTELLUNGSORT))
            }
            list.forEach { doc.addField(Field.DISPLAY_LIST, it) }
        }

        /* Return document. */
        doc
    }
}