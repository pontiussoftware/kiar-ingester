package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_INVENTORY_NUMBER
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that adds the necessary system fields to the [SolrInputDocument].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SystemTransformer(override val input: Source<SolrInputDocument>, parameters: Map<String,String>): Transformer<SolrInputDocument, SolrInputDocument> {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).map {
        it.addField(Constants.FIELD_NAME_CANTON, (it[FIELD_NAME_INVENTORY_NUMBER]?.value as String).substring(0..1))
        if (it[Constants.FIELD_NAME_CANTON]?.value == "BE") {  /* TODO: This is a hack! */
            it.addField(Constants.FIELD_NAME_OUTPUT, "mmBE Inventar")
        }
        it.addField(Constants.FIELD_NAME_IMAGECOUNT, it[Constants.FIELD_NAME_RAW]?.valueCount ?: 0) /* Set _imagecount_ field. */
        it
    }
}