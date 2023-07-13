package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.PaginatedResult

/**
 * A [PaginatedResult] of [JobLog] objects.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PaginatedJobLogResult (
    override val total: Int,
    override val page: Int,
    override val pageSize: Int,
    override val results: Array<JobLog>
): PaginatedResult<JobLog>