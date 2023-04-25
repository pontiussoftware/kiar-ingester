package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.sources.Source
import org.apache.solr.common.SolrInputDocument

/**
 * List of [Transformer]s supported by t
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Transformers {
    IMAGE,
    SYSTEM;

    /**
     * Generates and returns a new [Transformer] instance from this [Transformers] enumeration.
     *
     * @param input The input [Source]
     * @param parameters The map of named parameters.
     * @return [Transformer]
     */
    fun newInstance(input: Source<SolrInputDocument>, parameters: Map<String,String>) = when (this) {
        IMAGE -> ImageTransformer(input, parameters)
        SYSTEM -> SystemTransformer(input, parameters)
    }
}