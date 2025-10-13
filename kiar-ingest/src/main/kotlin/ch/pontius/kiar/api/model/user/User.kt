package ch.pontius.kiar.api.model.user

import ch.pontius.kiar.api.model.institution.Institution
import kotlinx.serialization.Serializable

typealias UserId = Int

/**
 * The [User] in the KIAR Tools API model.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class User(
    val id: UserId? = null,
    val username: String,
    val password: String? = null,
    val email: String? = null,
    val active: Boolean,
    val role: Role,
    val institution: Institution? = null,
    val createdAt: Long,
    val changedAt: Long
)