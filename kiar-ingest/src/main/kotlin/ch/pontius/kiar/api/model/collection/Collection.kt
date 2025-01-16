package ch.pontius.kiar.api.model.collection

import ch.pontius.kiar.api.model.institution.Institution
import kotlinx.serialization.Serializable

/**
 * An API representation of a {@link DbInstitution}.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Collection(
    val id: String? = null,
    val name: String,
    val displayName: String,
    val description: String,
    val images: List<String> = emptyList(),
    var institution: Institution? = null
)