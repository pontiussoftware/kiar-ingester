package ch.pontius.kiar.api.model.institution

import kotlinx.serialization.Serializable

/**
 * An API representation of a {@link DbInstitution}.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Institution(
    val id: String? = null,
    val name: String,
    val displayName: String,
    val participantName: String,
    val description: String? = null,
    val isil: String? = null,
    val street: String? = null,
    val zip: Int,
    val city: String,
    val canton: String,
    var longitude: Float? = null,
    var latitude: Float? = null,
    val email: String,
    val homepage: String? = null,
    val publish: Boolean,
    val availableCollections: List<String> = emptyList(),
    val selectedCollections: List<String> = emptyList(),
    val defaultRightStatement: String? = null,
    val defaultCopyright: String? = null,
    val imageName: String? = null,
    val createdAt: Long? = null,
    val changedAt: Long? = null,
)