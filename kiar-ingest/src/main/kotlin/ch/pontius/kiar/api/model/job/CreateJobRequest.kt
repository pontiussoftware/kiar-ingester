package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import kotlinx.serialization.Serializable

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class CreateJobRequest(val templateId: JobTemplateId, val jobName: String? = null)