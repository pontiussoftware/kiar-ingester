package ch.pontius.kiar.migration.database.job

import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.database.jobs.Jobs
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.*

/**
 * A log entry for a [DbJob] as maintained by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobLog(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbJobLog>() {
        fun migrate() {
            all().asSequence().forEach { dbJobLog ->
                JobLogs.insert {
                    it[jobId] = Jobs.idByName(dbJobLog.job.name) ?: throw IllegalStateException("Could not find job template with name '${dbJobLog.job.name}'.")
                    it[context] = JobLogContext.valueOf(dbJobLog.context.description)
                    it[level] = JobLogLevel.valueOf(dbJobLog.level.description)
                    it[documentId] = UUID.fromString(dbJobLog.documentId)
                    it[collectionId] = dbJobLog.collectionId
                    it[description] = dbJobLog.description
                }
            }
        }
    }

    /** The [DbJob] this [DbJobLog] belongs to. */
    var job: DbJob by xdParent(DbJob::log)

    /** The document ID this [DbJobLog] concerns. */
    var documentId by xdRequiredStringProp(trimmed = true)

    /** The collection ID this [DbJobLogLevel] concerns. */
    var collectionId by xdStringProp()

    /** The [DbJobLogContext]  of this [DbJobLog]. */
    var context by xdLink1(DbJobLogContext)

    /** The [DbJobLogLevel] of this [DbJobLog]. */
    var level by xdLink1(DbJobLogLevel)

    /** Description of the log entry. */
    var description by xdRequiredStringProp(trimmed = true)
}