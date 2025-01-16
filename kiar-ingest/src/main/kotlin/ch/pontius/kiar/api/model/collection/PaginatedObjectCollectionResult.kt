package ch.pontius.kiar.api.model.collection

import ch.pontius.kiar.api.model.PaginatedResult
import ch.pontius.kiar.api.model.institution.Institution
import kotlinx.serialization.Serializable

/**
 * A [PaginatedResult] of [Institution] objects.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
class PaginatedObjectCollectionResult(
    override val total: Int,
    override val page: Int,
    override val pageSize: Int,
    override val results: List<ObjectCollection>
): PaginatedResult<ObjectCollection>