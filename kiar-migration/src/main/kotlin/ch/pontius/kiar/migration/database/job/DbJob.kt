package ch.pontius.kiar.migration.database.job

import ch.pontius.kiar.migration.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.migration.database.institution.DbUser
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy

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

    /** The number of items processed by this [DbJob]. */
    var processed by xdLongProp()

    /** The number of items skipped by this [DbJob]. */
    var skipped by xdLongProp()

    /** The number of errors that occurred while processing this [DbJob]. */
    var error by xdLongProp()

    /** The [DbJobStatus] this [DbJob]. */
    var status by xdLink1(DbJobStatus)

    /** The [DbJobStatus] this [DbJob]. */
    var source by xdLink1(DbJobSource)

    /** The [DbJobTemplate] this [DbJob] has been created with. */
    var template by xdLink0_1(DbJobTemplate, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The date and time this [DbJob] was created. */
    var createdAt by xdRequiredDateTimeProp()

    /** The date and time this [DbJob] was last changed. */
    var changedAt by xdDateTimeProp()

    /** The [DbUser] that started this [DbJob]. */
    var createdByName by xdRequiredStringProp {  }

    /** The [DbUser] that started this [DbJob]. */
    var createdBy by xdLink0_1(DbUser)

    /** The [DbJobLog] entries associated with this [DbJob]. */
    val log by xdChildren0_N(DbJobLog::job)
}