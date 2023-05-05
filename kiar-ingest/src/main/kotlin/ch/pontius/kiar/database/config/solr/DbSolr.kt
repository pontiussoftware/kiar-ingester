package ch.pontius.kiar.database.config.solr

import ch.pontius.kiar.config.SolrConfig
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

    /** The URL held by this [DbSolr]. Must be unique!*/
    var server by xdRequiredStringProp(unique = true, trimmed = true)

    /** The username required to authenticate with the Apache Solr instance. */
    var username by xdStringProp(trimmed = true)

    /** The password required to authenticate with the Apache Solr instance. */
    var password by xdStringProp(trimmed = true)

    /** List of [DbCollection]s this [DbSolr] holds- */
    val collections by xdChildren0_N(DbCollection::solr)


    /**
     * A convenience method used to convert this [DbSolr] to a [SolrConfig]. Requires an ongoing transaction!
     *
     * @return [SolrConfig]
     */
    fun toApi() = SolrConfig(this.name, this.server, this.username, this.password, this.collections.asSequence().map { it.toApi() }.toList())
}