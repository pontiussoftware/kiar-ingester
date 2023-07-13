package ch.pontius.kiar.api.model.job

import kotlinx.serialization.Serializable

/**
 * A [JobLog] entry. Usually [JobLog]s describe events that occured during the processing of a particular [Job].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobLog(
    val jobId: String? = null,
    val documentId: String,
    val collectionId: String? = null,
    val context: JobLogContext,
    val level: JobLogLevel,
    val description: String
)