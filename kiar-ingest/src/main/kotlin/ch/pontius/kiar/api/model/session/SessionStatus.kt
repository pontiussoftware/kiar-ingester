package ch.pontius.kiar.api.model.session

import ch.pontius.kiar.api.model.user.Role
import kotlinx.serialization.Serializable

/**
 * A [SessionStatus] object as returned by the KIAR API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class SessionStatus(val username: String, val role: Role)