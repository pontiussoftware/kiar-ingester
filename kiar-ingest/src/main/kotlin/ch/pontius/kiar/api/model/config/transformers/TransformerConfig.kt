package ch.pontius.kiar.api.model.config.transformers

import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.transformers.ImageTransformer
import ch.pontius.kiar.ingester.processors.transformers.InstitutionTransformer
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
     * Generates and returns a new [TransformerConfig] instance from this [TransformerConfig] configuration.
     *
     * @param input The input [Source]
     * @return [TransformerConfig]
     */
    fun newInstance(input: Source<SolrInputDocument>) = when (this.type) {
        TransformerType.IMAGE -> ImageTransformer(input, this.parameters)
        TransformerType.SYSTEM -> InstitutionTransformer(input, this.parameters)
    }
}