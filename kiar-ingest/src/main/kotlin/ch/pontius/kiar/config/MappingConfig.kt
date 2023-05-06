package ch.pontius.kiar.config

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
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