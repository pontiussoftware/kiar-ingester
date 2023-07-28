package ch.pontius.kiar.api.model.config.transformers

import ch.pontius.kiar.database.config.transformers.DbTransformerType
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.transformers.DisplayTransformer
import ch.pontius.kiar.ingester.processors.transformers.ImageTransformer
import ch.pontius.kiar.ingester.processors.transformers.InstitutionTransformer
import org.apache.solr.common.SolrInputDocument

/**
 * Enumeration of [TransformerConfig]s supported by the KIAR tools
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class TransformerType {
    IMAGE,
    DISPLAY,
    SYSTEM;

    /**
     * Converts this [TransformerType] into a [DbTransformerType]. Requires an ongoing transaction.
     *
     * @return [DbTransformerType].
     */
    fun toDb(): DbTransformerType = when(this) {
        IMAGE -> DbTransformerType.IMAGE
        DISPLAY -> DbTransformerType.DISPLAY
        SYSTEM -> DbTransformerType.SYSTEM
    }
}