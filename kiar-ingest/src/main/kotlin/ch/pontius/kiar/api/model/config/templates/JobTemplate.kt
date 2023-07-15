package ch.pontius.kiar.api.model.config.templates

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.config.CollectionConfig
import kotlinx.serialization.Serializable

/**
 * A [JobTemplate] as returned by the KIAR Tools API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobTemplate(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val type: JobType,
    val startAutomatically: Boolean = false,
    val participantName: String,
    val solrConfigName: String,
    val entityMappingName: String,
    val createdAt: Long? = null,
    val changedAt: Long? = null,
    val transformers: List<TransformerConfig> = emptyList()
)