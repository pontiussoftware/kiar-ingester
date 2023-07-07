package ch.pontius.kiar.database.config.solr

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

    /** The username required to authenticate with the Apache Solr instance. */
    var username by xdStringProp(trimmed = true)

    /** The password required to authenticate with the Apache Solr instance. */
    var password by xdStringProp(trimmed = true)

    /** List of [DbCollection]s this [DbSolr] holds- */
    val collections by xdChildren0_N(DbCollection::solr)

    /**
     * A convenience method used to convert this [DbSolr] to a [ApacheSolrConfig]. Requires an ongoing transaction!
     *
     * @return [ApacheSolrConfig]
     */
    fun toApi() = ApacheSolrConfig(this.xdId, this.name, this.description, this.server, this.username, this.password, this.collections.asSequence().map { it.toApi() }.toList())
}