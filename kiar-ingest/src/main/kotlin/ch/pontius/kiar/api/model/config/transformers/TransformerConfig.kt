package ch.pontius.kiar.api.model.config.transformers

import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.transformers.DisplayTransformer
import ch.pontius.kiar.ingester.processors.transformers.HashUuidGenerator
import ch.pontius.kiar.ingester.processors.transformers.InstitutionTransformer
import ch.pontius.kiar.ingester.processors.transformers.RightsTransformer
import ch.pontius.kiar.ingester.processors.transformers.Transformer
import kotlinx.serialization.Serializable
import org.apache.solr.common.SolrInputDocument

/**
 * A [TransformerConfig] configuration.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class TransformerConfig(val type: TransformerType, val parameters: Map<String,String>) {
    /**
     * Generates and returns a new [Transformer] instance from this [TransformerConfig] entry.
     *
     * @param input The input [Source] to hook the [Transformer] to.
     * @return [Transformer]
     */
    fun newInstance(input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {
        val parameters = this.parameters.asSequence().associate { it.key to it.value }
        return when (this.type) {
            TransformerType.DISPLAY -> DisplayTransformer(input)
            TransformerType.SYSTEM -> InstitutionTransformer(input)
            TransformerType.RIGHTS -> RightsTransformer(input)
            TransformerType.UUID -> HashUuidGenerator(input, parameters)
        }
    }
}