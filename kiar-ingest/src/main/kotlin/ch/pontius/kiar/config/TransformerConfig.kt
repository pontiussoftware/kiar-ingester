package ch.pontius.kiar.config

import ch.pontius.kiar.api.model.config.transformers.TransformerType

/**
 * A configuration for applying [TransformerType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class TransformerConfig(val type: TransformerType, val parameters: Map<String,String> = emptyMap())