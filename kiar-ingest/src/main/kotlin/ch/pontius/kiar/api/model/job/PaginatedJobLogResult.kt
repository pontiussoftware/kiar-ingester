package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.PaginatedResult
import kotlinx.serialization.Serializable

/**
 * A [PaginatedResult] of [JobLog] objects.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class PaginatedJobLogResult (
    override val total: Int,
    override val page: Int,
    override val pageSize: Int,
    override val results: Array<JobLog>
): PaginatedResult<JobLog>