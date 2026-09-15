package ch.pontius.kiar.api

import ch.pontius.kiar.api.model.user.User
import kotlinx.serialization.Serializable

/**
 * The server-side session state associated with a logged-in [User].
 *
 * The session itself is stored server-side; the client only receives an opaque session ID cookie.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class UserSession(val userId: Int, val username: String)
