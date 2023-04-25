package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.ingester.parsing.values.ValueParsers

/**
 * A [XmlAttributeMapping] definition defines how an individual XML attribute should be mapped to a destination attribute.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class XmlAttributeMapping(
    val source: String,
    val destination: String,
    val parser: ValueParsers,
    val required: Boolean = false,
    val multiValued: Boolean = false,
    val parameters: Map<String,String> = emptyMap(),
)