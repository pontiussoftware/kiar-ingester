package ch.pontius.kiar.api.model.institution

import ch.pontius.kiar.api.model.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * A [PaginatedResult] of [Institution] objects.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class PaginatedInstitutionResult(
    override val total: Long,
    override val page: Int,
    override val pageSize: Int,
    override val results: List<Institution>
): PaginatedResult<Institution>