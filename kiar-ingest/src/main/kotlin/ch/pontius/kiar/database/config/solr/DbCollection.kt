package ch.pontius.kiar.database.config.solr

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A named Apache Solr collection managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbCollection(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbCollection>()

    /** The name held by this [DbCollection]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The [DbCollectionType] of this [DbCollection]*/
    var type by xdLink1(DbCollectionType)

    /** [DbSolr] instance this [DbCollection] belongs to. */
    var solr: DbSolr by xdParent(DbSolr::collections)
}