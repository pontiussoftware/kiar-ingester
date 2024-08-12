package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.utilities.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

/**
 * A [JobLog] entry. Usually [JobLog]s describe events that occured during the processing of a particular [Job].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobLog(
    val jobId: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val documentId: UUID? = null,
    val collectionId: String? = null,
    val context: JobLogContext,
    val level: JobLogLevel,
    val description: String
)