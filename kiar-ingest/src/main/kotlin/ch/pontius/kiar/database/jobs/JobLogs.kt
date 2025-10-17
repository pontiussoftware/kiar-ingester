package ch.pontius.kiar.database.jobs

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * A [IntIdTable] that holds information about [JobLogs].
 *
 * [JobLogs] entries represent logs created as part of a [Jobs] entry
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object JobLogs: Table("jobs_logs") {

    /** The [Jobs] entry this [JobLogs] entry belongs to. */
    val jobId = reference("job_id", Jobs, ReferenceOption.CASCADE)

    /** The [JobLogLevel] of a [JobLogs] entry. */
    val level = enumerationByName("level", 16, JobLogLevel::class)

    /** The [JobLogContext] of a [JobLogs] entry. */
    val context = enumerationByName("context", 16, JobLogContext::class)

    /** The ID of an Apache Solr document a [JobLogs] entry concerns. */
    val documentId = uuid("document_id").nullable()

    /** The name of an Apache Solr colletion a [JobLogs] entry concerns. */
    val collectionId = varchar("collection_id", 128).nullable()

    /** The description of the [JobLogs]  entry. */
    val description = text("description")

    /** Timestamp of creation of the [Jobs] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /**
     * Converts this [ResultRow] into an [JobLog].
     *
     * @return [JobLog]
     */
    fun ResultRow.toJobLog() = JobLog(
        jobId = this[jobId].value,
        documentId = this[documentId]?.toString(),
        collectionId = this[collectionId],
        context = this[context],
        level = this[level],
        description = this[description]
    )
}