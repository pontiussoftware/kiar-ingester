package ch.pontius.kiar.api.model.user

import ch.pontius.kiar.api.model.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * A [PaginatedResult] of [User] objects.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class PaginatedUserResult(
    override val total: Int,
    override val page: Int,
    override val pageSize: Int,
    override val results: List<User>
): PaginatedResult<User>