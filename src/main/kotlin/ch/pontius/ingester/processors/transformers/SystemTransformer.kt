package ch.pontius.ingester.processors.transformers

import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.solrj.Constants
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_INVENTORY_NUMBER
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
    override fun toFlow(): Flow<SolrInputDocument> = this.input.toFlow().map {
        it.addField(Constants.FIELD_NAME_CANTON, (it[FIELD_NAME_INVENTORY_NUMBER]?.value as String).substring(0..1))
        it.removeField(Constants.FIELD_NAME_RAW) /* Remove the raw image field. */
        it
    }
}