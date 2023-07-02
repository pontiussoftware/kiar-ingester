package ch.pontius.kiar.database.job

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A log entry for a [DbJob] as maintained by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobLog(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbJobLog>()

    /** The [DbJob] this [DbJobLog] belongs to. */
    var job: DbJob by xdParent(DbJob::log)

    /** The document ID this [DbJobLog] concerns. */
    var documentId by xdRequiredStringProp(trimmed = true)

    /** The [DbJobLogContext]  ofthis [DbJobLog]. */
    var context by xdLink1(DbJobLogContext)

    /** The [DbJobLogLevel] of this [DbJobLog]. */
    var level by xdLink1(DbJobLogLevel)

    /** Description of the log entry. */
    var description by xdRequiredStringProp(trimmed = true)
}