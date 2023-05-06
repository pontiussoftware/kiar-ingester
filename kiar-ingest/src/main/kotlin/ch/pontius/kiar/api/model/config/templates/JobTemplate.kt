package ch.pontius.kiar.api.model.config.templates

import ch.pontius.kiar.api.model.config.templates.JobType
import kotlinx.serialization.Serializable

/**
 * A [JobTemplate] as returned by the KIAR Tools API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobTemplate(val name: String, val type: JobType, val participant: String, val solr: String, val mapping: String)