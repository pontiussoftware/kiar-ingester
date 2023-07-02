package ch.pontius.kiar.api.model.job

import kotlinx.serialization.Serializable

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class CreateJobRequest(val templateId: String, val jobName: String? = null)