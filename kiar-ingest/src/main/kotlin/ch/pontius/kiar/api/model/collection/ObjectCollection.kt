package ch.pontius.kiar.api.model.collection

import ch.pontius.kiar.api.model.institution.Institution
import kotlinx.serialization.Serializable

typealias CollectionId = Int

/**
 * An API representation of a {@link DbInstitution}.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ObjectCollection(
    val id: CollectionId? = null,
    val uuid: String? = null,
    val name: String,
    val displayName: String,
    val publish: Boolean,
    val description: String,
    val institution: Institution? = null,
    val filters: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val createdAt: Long? = null,
    val changedAt: Long? = null
)