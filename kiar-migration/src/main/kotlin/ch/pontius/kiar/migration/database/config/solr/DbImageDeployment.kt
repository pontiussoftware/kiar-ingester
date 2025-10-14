package ch.pontius.kiar.migration.database.config.solr

import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.SolrConfigs
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.constraints.url
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A (global) configuration for image deployment.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbImageDeployment(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbImageDeployment>()  {
        fun migrate() {
            all().asSequence().forEach { dbImageDeployment ->
                ImageDeployments.insert {
                    it[solrInstanceId] = SolrConfigs.idByName(dbImageDeployment.solr.name) ?: throw IllegalArgumentException("Could not find Apache Solr config with name '${dbImageDeployment.solr.name}'.")
                    it[name] = dbImageDeployment.name
                    it[path] = dbImageDeployment.path
                    it[format] = ImageFormat.valueOf(dbImageDeployment.format.description)
                    it[src] = dbImageDeployment.source
                    it[server] = dbImageDeployment.server
                    it[maxSize] = dbImageDeployment.maxSize
                }
            }
        }
    }

    /** The name held by this [DbImageDeployment] configuration. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbImageFormat] produced by this [DbImageDeployment]. */
    var format by xdLink1(DbImageFormat)

    /** The source field used by  this [DbImageDeployment]. */
    var source by xdRequiredStringProp(trimmed = true)

    /** The (local) deployment path. */
    var path by xdRequiredStringProp(trimmed = true)

    /** The (public) deployment URL. Can be null (for relative paths). */
    var server by xdStringProp(trimmed = true) { url() }

    /** The maximum size of the resulting image. */
    var maxSize by xdRequiredIntProp()

    /** The [DbSolr] this [DbImageDeployment] belongs to. */
    var solr: DbSolr by xdParent(DbSolr::deployments)
}