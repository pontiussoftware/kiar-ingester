package ch.pontius.kiar.migration.database.config.transformers

import ch.pontius.kiar.api.model.config.transformers.TransformerType
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.Transformers
import ch.pontius.kiar.migration.database.config.jobs.DbJobTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A configuration for an input data transformer.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformer(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTransformer>() {
        fun migrate() {
            all().asSequence().forEach { dbTransformer ->
                Transformers.insert {
                    it[jobTemplateId] = JobTemplates.idByName(dbTransformer.template.name) ?: throw IllegalStateException("Could not find job template with name '${dbTransformer.template.name}'.")
                    it[parameters] = dbTransformer.parameters.asSequence().map { m -> m.key to m.value }.toMap()
                    it[type] = TransformerType.valueOf(dbTransformer.type.description)
                }
            }
        }
    }


    /** The [DbTransformerType] of this [DbTransformer]. */
    var type by xdLink1(DbTransformerType)

    /** The value of this [DbTransformerParameter]. */
    val parameters by xdChildren0_N(DbTransformerParameter::transformer)

    /** The [DbJobTemplate] this [DbTransformer] belongs to. */
    val template: DbJobTemplate by xdParent(DbJobTemplate::transformers)
}