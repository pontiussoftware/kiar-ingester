package ch.pontius.kiar.migration.database.config.solr

import ch.pontius.kiar.database.config.SolrConfigs
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * An Apache Solr instance managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbSolr(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbSolr>() {
        fun migrate() {
            all().asSequence().forEach { dbSolr ->
                SolrConfigs.insert {
                    it[name] = dbSolr.name
                    it[description] = dbSolr.description
                    it[server] = dbSolr.server
                    it[publicServer] = dbSolr.publicServer
                    it[username] = dbSolr.username
                    it[password] = dbSolr.password
                    it[created] = dbSolr.createdAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                    it[modified] = dbSolr.changedAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                }
            }
        }
    }

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