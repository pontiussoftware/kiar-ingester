package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.database.config.AttributeMappings.toAttributeMapping
import ch.pontius.kiar.database.config.EntityMappings.toEntityMapping
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.database.config.SolrCollections.toSolrCollection
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.database.config.Transformers.toTransformerConfig
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * A [Table] that holds information about [JobTemplates] that can be used to create jobs
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object JobTemplates: IntIdTable("jobs_templates") {
    /** A reference to a [Participants] entry, a [JobTemplates] belongs to. */
    val participantId = reference("participant_id", Participants,ReferenceOption.RESTRICT, onUpdate = ReferenceOption.CASCADE)

    /** Reference to the [EntityMappings] entry a [JobTemplates] entry employs. */
    val entityMappingId = reference("entity_mapping_id", EntityMappings, ReferenceOption.RESTRICT, onUpdate = ReferenceOption.CASCADE)

    /** Reference to the [SolrConfigs] entry a [JobTemplates] entry employs. */
    val solrId = reference("solr_config_id", SolrConfigs, ReferenceOption.RESTRICT, onUpdate = ReferenceOption.CASCADE)

    /** The name of the [JobTemplates] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The optional description of the [JobTemplates] entry. */
    val description = text("description").nullable()

    /** The [JobType] of a [JobTemplates] entry.*/
    val type = enumerationByName("type", 16, JobType::class)

    /** A flag indicating, that jobs derived from the [JobTemplates] entry should start automatically. */
    val startAutomatically = bool("start_automatically").default(false)

    /** Timestamp of creation of the [Institutions] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [Institutions] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [JobTemplates] [id] by its [name].
     *
     * @param name The name to lookup
     * @return [JobTemplates] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = JobTemplates.select(id).where { JobTemplates.name eq name }.map { it[id] }.firstOrNull()

    /**
     * Finds a [JobTemplate] by its [JobTemplateId].
     *
     * @param templateId The [JobTemplateId] to look for.
     * @return Resulting [JobTemplate] or null
     */
    fun getById(templateId: JobTemplateId): JobTemplate? {
        val template = (JobTemplates innerJoin EntityMappings innerJoin SolrConfigs innerJoin Participants)
            .selectAll()
            .where { id eq templateId }
            .map { it.toJobTemplate() }
            .firstOrNull()

        /* Abort, if template was not found. */
        if (template == null) return null

        /* Obtain Apache Solr Configuration with collections. */
        val solr = template.config ?: throw IllegalStateException("Failed to obtain Apache Solr configuration for template with ID ${template.id}.")
        val collections = SolrCollections.selectAll().where { SolrCollections.solrInstanceId eq solr.id }.map { it.toSolrCollection() }
        val deployments = ImageDeployments.selectAll().where { ImageDeployments.solrInstanceId eq solr.id }.map { it.toImageDeployment() }

        /* Obtain entity mapping. */
        val mapping = template.mapping ?: throw IllegalStateException("Failed to obtain entity mapping configuration for job with ID ${template.id}.")
        val attributes = AttributeMappings.selectAll().where { AttributeMappings.entityMappingId eq mapping.id }.map { it.toAttributeMapping() }

        /* Obtain transformers. */
        val transformers = Transformers.selectAll()
            .where { Transformers.jobTemplateId eq template.id }
            .orderBy(Transformers.order, SortOrder.ASC)
            .map { it.toTransformerConfig() }

        /* Return copy of template. */
        return template.copy(transformers = transformers, config = solr.copy(collections = collections, deployments = deployments), mapping = mapping.copy(attributes = attributes))
    }

    /**
     * Converts this [ResultRow] into an [JobTemplate].
     *
     * @return [JobTemplate]
     */
    fun ResultRow.toJobTemplate() = JobTemplate(
        id = this[id].value,
        name = this[name],
        description = this[description],
        type = this[type],
        startAutomatically = this[startAutomatically],
        participantName = this[Participants.name],
        config = this.getOrNull(SolrConfigs.id)?.let { this.toSolr() },
        mapping = this.getOrNull(EntityMappings.id)?.let { this.toEntityMapping() },
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli(),
    )
}