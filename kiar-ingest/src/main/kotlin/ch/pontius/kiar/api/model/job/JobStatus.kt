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
    SCHEDULED,
    INTERRUPTED,
    INGESTED,
    FAILED;
}