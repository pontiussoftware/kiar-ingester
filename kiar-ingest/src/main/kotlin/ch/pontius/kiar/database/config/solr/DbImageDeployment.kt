package ch.pontius.kiar.database.config.solr

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.database.config.jobs.DbJobType
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

    /** The (local) deployment path. */
    var path by xdRequiredStringProp(trimmed = true)

    /** The (public) deployment URL. */
    var server by xdStringProp(trimmed = true) { url() }

    /** The maximum size of the resulting image. */
    var maxSize by xdRequiredIntProp()

    /** The [DbSolr] this [DbImageDeployment] belongs to. */
    var solr: DbSolr by xdParent(DbSolr::deployments)

    /**
     * Convenience method to convert this [DbJobType] to a [JobType].
     *
     * Requires an ongoing transaction.
     *
     * @return [JobType]
     */
    fun toApi() = ImageDeployment(
        this.name,
        this.format.toApi(),
        this.path,
        this.server,
        this.maxSize
    )
}