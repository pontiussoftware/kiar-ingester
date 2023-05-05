package ch.pontius.kiar.database.config.solr

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdChildren0_N
import kotlinx.dnq.xdRequiredStringProp

/**
 * An Apache Solr instance managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbSolr(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbSolr>()

    /** The name held by this [DbSolr]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The URL held by this [DbSolr]. Must be unique!*/
    var server by xdRequiredStringProp(unique = true, trimmed = true)

    /** List of [DbCollection]s this [DbSolr] holds- */
    val collections by xdChildren0_N(DbCollection::solr)
}