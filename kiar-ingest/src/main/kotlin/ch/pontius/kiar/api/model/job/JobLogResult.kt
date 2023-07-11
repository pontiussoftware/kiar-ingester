package ch.pontius.kiar.api.model.job

import kotlinx.serialization.Serializable

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobLogResult(val total: Int, val page: Int, val pageSize: Int, val logs: Array<JobLog>)