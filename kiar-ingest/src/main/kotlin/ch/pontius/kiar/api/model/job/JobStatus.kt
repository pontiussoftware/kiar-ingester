package ch.pontius.kiar.api.model.job

/**
 * The status of a [Job].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class JobStatus {
    CREATED,
    ABORTED,
    HARVESTED,
    RUNNING,
    INTERRUPTED,
    INGESTED,
    FAILED;
}