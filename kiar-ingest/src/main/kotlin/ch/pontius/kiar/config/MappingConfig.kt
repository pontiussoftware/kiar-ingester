package ch.pontius.kiar.config

import ch.pontius.kiar.ingester.parsing.xml.AttributeMapping
import kotlinx.serialization.Serializable

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class MappingConfig(
    val name: String,
    val description: String? = null,
    val values: List<AttributeMapping> = emptyList()
)