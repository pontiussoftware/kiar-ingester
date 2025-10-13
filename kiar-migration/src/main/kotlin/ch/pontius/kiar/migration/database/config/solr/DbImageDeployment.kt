package ch.pontius.kiar.migration.database.config.solr

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.constraints.url
import kotlinx.dnq.*

/**
 * A (global) configuration for image deployment.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbImageDeployment(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbImageDeployment>()

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