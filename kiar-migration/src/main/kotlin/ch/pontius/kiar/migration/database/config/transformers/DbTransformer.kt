package ch.pontius.kiar.migration.database.config.transformers

import ch.pontius.kiar.migration.database.config.jobs.DbJobTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A configuration for an input data transformer.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformer(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTransformer>()

    /** The [DbTransformerType] of this [DbTransformer]. */
    var type by xdLink1(DbTransformerType)

    /** The value of this [DbTransformerParameter]. */
    val parameters by xdChildren0_N(DbTransformerParameter::transformer)

    /** The [DbJobTemplate] this [DbTransformer] belongs to. */
    val template: DbJobTemplate by xdParent(DbJobTemplate::transformers)
}