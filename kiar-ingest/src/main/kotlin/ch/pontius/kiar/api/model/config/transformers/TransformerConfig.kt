package ch.pontius.kiar.api.model.config.transformers

import kotlinx.serialization.Serializable

/**
 * A [TransformerConfig] configuration.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class TransformerConfig(val type: TransformerType, val parameters: Map<String,String>)