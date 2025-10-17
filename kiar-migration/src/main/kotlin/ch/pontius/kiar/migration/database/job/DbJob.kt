package ch.pontius.kiar.migration.database.job

import ch.pontius.kiar.api.model.job.JobSource
import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.jobs.Jobs
import ch.pontius.kiar.migration.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.migration.database.institution.DbUser
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * A [DbJob] as managed by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJob(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbJob>() {
        fun migrate() {
            all().asSequence().forEach { dbJob ->
                Jobs.insert {
                    it[templateId] = dbJob.template?.name?.let { n -> JobTemplates.idByName(n) ?: throw IllegalStateException("Could not find job template with name '$n'.") }
                    it[userId] = dbJob.createdBy?.name?.let { n -> Users.idByName(n) ?: throw IllegalStateException("Could not find job template with name '$n'.") }

                    it[name] = dbJob.name
                    it[status] = JobStatus.valueOf(dbJob.status.description)
                    it[src] = JobSource.valueOf(dbJob.source.description)
                    it[processed] = dbJob.processed
                    it[error] = dbJob.skipped
                    it[skipped] = dbJob.error
                    it[created] = Instant.ofEpochMilli(dbJob.createdAt.millis)
                    it[modified] = dbJob.changedAt?.millis?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
                    it[createdBy] = dbJob.createdByName
                }
            }
        }
    }

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