package ch.pontius.kiar.database.ingest

import ch.pontius.kiar.database.institution.DbUser
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A [DbJob] as managed by the KIAR Uploader Tool.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJob(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbJob>()

    /** Name of this [DbJob]. */
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbTaskStatus] this [DbJob]. */
    var status by xdLink1(DbTaskStatus)

    /** The date and time this [DbJob] was created. */
    var createdAt by xdRequiredDateTimeProp()

    /** The [DbUser] that started this [DbJob]. */
    var createdBy by xdLink1(DbUser)
}