package ch.pontius.kiar.api.model.user

import kotlinx.serialization.Serializable

/**
 * The [Role] a [User] can have.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
enum class Role {
    ADMINISTRATOR,
    MANAGER,
    VIEWER;
}
