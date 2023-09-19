package ch.pontius.kiar.api.model.config.mappings

import kotlinx.serialization.Serializable

/**
 * A [EntityMapping] as exposed and used by the KIAR API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class EntityMapping(
    val id: String? = null,
    val name: String,
    val description: String?,
    val type: MappingFormat,
    val createdAt: Long? = null,
    val changedAt: Long? = null,
    val attributes: List<AttributeMapping>
)