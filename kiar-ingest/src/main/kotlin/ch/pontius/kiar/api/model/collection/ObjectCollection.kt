package ch.pontius.kiar.api.model.collection

import kotlinx.serialization.Serializable

/**
 * An API representation of a {@link DbInstitution}.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ObjectCollection(
    val id: String? = null,
    val name: String,
    val displayName: String,
    val publish: Boolean,
    val institutionName: String,
    val description: String,
    val filters: List<String> = emptyList(),
    val images: List<String> = emptyList()
)