package ch.pontius.kiar.api.model.job

/**
 * The [JobLogLevel] of a [JobLog] entry.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
enum class JobLogLevel {
    WARNING,
    ERROR,
    VALIDATION,
    SEVERE;
}