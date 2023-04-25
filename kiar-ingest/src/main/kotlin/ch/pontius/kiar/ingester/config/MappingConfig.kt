package ch.pontius.kiar.ingester.config

import ch.pontius.kiar.ingester.parsing.xml.XmlAttributeMapping

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class MappingConfig(
    val name: String,
    val description: String = "",
    val values: List<XmlAttributeMapping> = emptyList()
)