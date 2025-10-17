package ch.pontius.kiar.database.jobs

import ch.pontius.kiar.api.model.job.Job
import ch.pontius.kiar.api.model.job.JobId
import ch.pontius.kiar.api.model.job.JobSource
import ch.pontius.kiar.api.model.job.JobStatus
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.JobTemplates.toJobTemplate
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.institutions.Users
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * A [IntIdTable] that holds information about [Jobs].
 *
 * [Jobs] entries represent ingest activity started by some user.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Jobs: IntIdTable("jobs") {

    /** The [JobTemplates] entry this [Jobs] entry is derived from. */
    val templateId = reference("template_id", JobTemplates, onDelete = ReferenceOption.SET_NULL).nullable()

    /** The [Users] entry this [Jobs] entry belongs to. */
    val userId = reference("user_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()

    /** The name of the [Jobs] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** Optional comment for the [Jobs] entry. */
    val comment = text("comment").nullable()

    /** The [JobStatus] of a [Jobs] entry. */
    val status = enumerationByName("status", 16, JobStatus::class)

    /** The [JobSource] ] of a [Jobs] entry. */
    val src = enumerationByName("source", 8, JobSource::class)

    /** The number of processed elements for a [Jobs] entry. */
    val processed = long("processed").default(0L)

    /** The number of skipped elements for a [Jobs] entry. */
    val skipped = long("skipped").default(0L)

    /** The number of erroneous elements for a [Jobs] entry. */
    val error = long("error").default(0L)

    /** Timestamp of creation of the [Jobs] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of creation of the [Jobs] entry. */
    val createdBy = varchar("created_by", 32)

    /** Timestamp of change of the [Jobs] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [Jobs] [id] by its [name].
     *
     * @param name The name to lookup
     * @return [Jobs] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = Jobs.select(id).where { Jobs.name eq name }.map { it[id] }.firstOrNull()


    /**
     * Finds a [Job] by its [JobId].
     *
     * @param jobId The [JobId] to look for.
     * @return Resulting [Job] or null
     */
    fun getById(jobId: JobId) = (Jobs innerJoin JobTemplates innerJoin EntityMappings innerJoin SolrConfigs innerJoin Participants)
        .selectAll()
        .where { id eq jobId }
        .map { it.toJob() }
        .firstOrNull()

    /**
     * Converts this [ResultRow] into an [Job].
     *
     * @return [Job]
     */
    fun ResultRow.toJob() = Job(
        id = this[id].value,
        name = this[name],
        template = this.getOrNull(JobTemplates.id).let { this.toJobTemplate() },
        status = this[status],
        source = this[src],
        processed = this[processed],
        skipped = this[skipped],
        error = this[error],
        createdAt = this[created].toEpochMilli(),
        createdBy = this[createdBy],
        changedAt = this[modified].toEpochMilli(),
    )
}