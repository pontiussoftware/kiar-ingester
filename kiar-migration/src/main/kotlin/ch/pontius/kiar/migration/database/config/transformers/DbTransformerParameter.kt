package ch.pontius.kiar.migration.database.config.transformers

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A parameter used to configure a [DbTransformer]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformerParameter(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTransformerParameter>()

    /** The key of this [DbTransformerParameter]. */
    var key by xdRequiredStringProp()

    /** The value of this [DbTransformerParameter]. */
    var value by xdRequiredStringProp()

    /** The value of this [DbTransformerParameter]. */
    var transformer: DbTransformer by xdParent(DbTransformer::parameters)
}