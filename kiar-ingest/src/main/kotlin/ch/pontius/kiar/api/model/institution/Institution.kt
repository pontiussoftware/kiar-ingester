package ch.pontius.kiar.api.model.institution

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.masterdata.RightStatement
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
    val email: String,
    val homepage: String? = null,
    val publish: Boolean,
    val availableCollections: List<ApacheSolrCollection> = emptyList(),
    val selectedCollections: List<ApacheSolrCollection> = emptyList(),
    val defaultLicense: RightStatement? = null,
    val defaultCopyright: String? = null,
    val createdAt: Long? = null,
    val changedAt: Long? = null,
)