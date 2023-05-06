package ch.pontius.kiar.api.model.session

import kotlinx.serialization.Serializable

/**
 * A request sent for a user login as used by the KIAR API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class LoginRequest(val username: String, val password: String)