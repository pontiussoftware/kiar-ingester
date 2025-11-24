package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.templates.JobTemplateId
import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.api.model.config.transformers.TransformerType
import ch.pontius.kiar.database.config.Transformers.jobTemplateId
import ch.pontius.kiar.database.config.Transformers.type
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.json

/**
 * A [org.jetbrains.exposed.v1.core.Table] that holds information about [Transformers] and their configuration.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Transformers: Table("template_transformers") {
    /** Reference to the [JobTemplates] entry a [Transformers] entry belongs to. */
    val jobTemplateId = reference("job_template_id", JobTemplates, ReferenceOption.CASCADE)

    /** The [TransformerType] of a [Transformers] entry. */
    val type = enumerationByName("type", 16, TransformerType::class)

    /** The order of the transformer entry. */
    val order = integer("order")

    /** Optional configuration parameters for a [Transformers] entry. */
    val parameters = json<Map<String,String>>("parameters", Json).nullable()

    /** The combination of [jobTemplateId] and [type] is primary key. */
    override val primaryKey = PrimaryKey(this.jobTemplateId, this.type)

    /**
     * Finds all [TransformerConfig]s for a given [JobTemplateId].
     *
     * @param jobTemplateId The [JobTemplateId] to search for.
     * @return [List] of [TransformerConfig]s.
     */
    fun getByJobTemplateId(jobTemplateId: JobTemplateId): List<TransformerConfig> = Transformers.selectAll().where {
        Transformers.jobTemplateId eq jobTemplateId
    }.orderBy(order, SortOrder.ASC).map {
        it.toTransformerConfig()
    }

    /**
     * Converts this [ResultRow] into an [TransformerConfig].
     *
     * @return [TransformerConfig]
     */
    fun ResultRow.toTransformerConfig() = TransformerConfig(
        type = this[type],
        parameters = this[parameters] ?: emptyMap()
    )
}