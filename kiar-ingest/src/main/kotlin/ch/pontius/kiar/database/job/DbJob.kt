package ch.pontius.kiar.database.job

import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.institution.DbUser
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A [DbJob] as managed by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJob(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbJob>()

    /** Name of this [DbJob]. */
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbJobStatus] this [DbJob]. */
    var status by xdLink1(DbJobStatus)

    /** The [DbJobStatus] this [DbJob]. */
    var source by xdLink1(DbJobSource)

    /** The [DbJobTemplate] this [DbJob] has been created with. */
    var template by xdLink0_1(DbJobTemplate)

    /** The date and time this [DbJob] was created. */
    var createdAt by xdRequiredDateTimeProp()

    /** The [DbUser] that started this [DbJob]. */
    var createdBy by xdLink0_1(DbUser)
}