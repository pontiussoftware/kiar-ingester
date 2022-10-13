package ch.pontius.ingester.config

import ch.pontius.ingester.processors.transformers.Transformers

/**
 * A configuration for applying [Transformers].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class TransformerConfig(val type: Transformers, val parameters: Map<String,String> = emptyMap())