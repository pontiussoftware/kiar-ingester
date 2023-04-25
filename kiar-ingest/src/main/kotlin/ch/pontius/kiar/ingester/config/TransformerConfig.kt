package ch.pontius.kiar.ingester.config

import ch.pontius.kiar.ingester.processors.transformers.Transformers

/**
 * A configuration for applying [Transformers].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class TransformerConfig(val type: Transformers, val parameters: Map<String,String> = emptyMap())