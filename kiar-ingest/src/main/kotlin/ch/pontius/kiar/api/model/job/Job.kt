package ch.pontius.kiar.api.model.job

import kotlinx.serialization.Serializable

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Job(
    /** The (optional) database ID of this [Job]. */
    val id: String? = null,

    /** The name of this [Job]. */
    val name: String,

    /** The [JobStatus] this [Job]. */
    val status: JobStatus,

    /** The [JobSource] this [Job]. */
    val source: JobSource,

    /** The [JobTemplate] of this [Job]. */
    val templateName: String? = null,

    /** The entries processed by this [Job]. */
    val processed: Long,

    /** The entries skipped by this [Job]. */
    val skipped: Long,

    /** The number of processing errors encountered by this [Job]. */
    val error: Long,

    /** Timestamp of this [Job]'s creation. */
    val createdAt: Long,

    /** Name of the user who created this [Job]. */
    val createdBy: String
)