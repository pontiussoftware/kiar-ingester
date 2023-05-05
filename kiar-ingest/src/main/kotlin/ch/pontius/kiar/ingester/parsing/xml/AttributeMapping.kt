package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.ingester.parsing.values.ValueParsers
import kotlinx.serialization.Serializable

/**
 * A [AttributeMapping] definition defines how an individual XML attribute should be mapped to a destination attribute.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class AttributeMapping(
    val source: String,
    val destination: String,
    val parser: ValueParsers,
    val required: Boolean = false,
    val multiValued: Boolean = false,
    val parameters: Map<String,String> = emptyMap(),
)