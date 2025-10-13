package ch.pontius.kiar.migration.database.config.solr

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

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

    /** An optional description of this Apache Solr configuration. */
    var description by xdStringProp(trimmed = true)

    /** The URL held by this [DbSolr]. Must be unique!*/
    var server by xdRequiredStringProp(unique = true, trimmed = true)

    /** The public URL held by this [DbSolr]. Can be null, in which case the regular server URL can be used.*/
    var publicServer by xdStringProp(trimmed = true)

    /** The username required to authenticate with the Apache Solr instance. */
    var username by xdStringProp(trimmed = true)

    /** The password required to authenticate with the Apache Solr instance. */
    var password by xdStringProp(trimmed = true)

    /** The date and time this [DbSolr] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbSolr] was last changed. */
    var changedAt by xdDateTimeProp()

    /** List of [DbCollection]s this [DbSolr] holds- */
    val collections by xdChildren0_N(DbCollection::solr)

    /** List of [DbCollection]s this [DbSolr] holds- */
    val deployments by xdChildren0_N(DbImageDeployment::solr)
}