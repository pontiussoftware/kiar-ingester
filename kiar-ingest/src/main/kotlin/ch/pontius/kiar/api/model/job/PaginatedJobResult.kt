package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * A [PaginatedResult] of [Job] objects.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class PaginatedJobResult (
    override val total: Int,
    override val page: Int,
    override val pageSize: Int,
    override val results: List<Job>
): PaginatedResult<Job>