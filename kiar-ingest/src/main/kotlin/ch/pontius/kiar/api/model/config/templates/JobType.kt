package ch.pontius.kiar.api.model.config.templates

/**
 * Enumeration of the available [JobType].
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
enum class JobType(val suffix: String) {
    XML("xml"),
    JSON("json"),
    EXCEL("xlsx"),
    KIAR("kiar");
}