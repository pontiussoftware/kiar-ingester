package ch.pontius.kiar.api.model.session

import kotlinx.serialization.Serializable

/**
 * The [User] in the KIAR Tools API model.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class User(val id: String? = null, val username: String, val password: String? = null, val email: String? = null, val role: Role, val institution: String? = null)