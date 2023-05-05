package ch.pontius.kiar.database.config.transformers

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdParent
import kotlinx.dnq.xdStringProp

/**
 * A parameter used to configure a [DbTransformer]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformerParameter(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTransformerParameter>()

    /** The key of this [DbTransformerParameter]. */
    var key by xdStringProp()

    /** The value of this [DbTransformerParameter]. */
    var value by xdStringProp()

    /** The value of this [DbTransformerParameter]. */
    var transformer: DbTransformer by xdParent(DbTransformer::parameters)
}