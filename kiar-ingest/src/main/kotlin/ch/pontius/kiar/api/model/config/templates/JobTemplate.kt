package ch.pontius.kiar.api.model.config.templates

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.SolrConfigs
import kotlinx.serialization.Serializable
import java.nio.file.Path

typealias JobTemplateId = Int

/**
 * A [JobTemplate] as returned by the KIAR Tools API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobTemplate(
    val id: JobTemplateId? = null,
    val name: String,
    val description: String? = null,
    val type: JobType,
    val startAutomatically: Boolean = false,
    val participantName: String,
    val createdAt: Long,
    val changedAt: Long,
    val config: ApacheSolrConfig? = null,
    val mapping: EntityMapping? = null,
    val transformers: List<TransformerConfig> = emptyList()
) {
    /**
     * Returns the [Path] to the expected ingest file for this [JobTemplate].
     *
     * @param config The [Config] instance required to resolve the [Path].
     * @return Resulting [Path]
     */
    fun sourcePath(config: Config): Path = config.ingestPath.resolve(this.participantName).resolve("${this.name}.${this.type.suffix}")
}