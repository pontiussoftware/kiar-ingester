package ch.pontius.kiar.kiar

/**
 * Supported types of [KiarEntry].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class KiarEntryType(val suffix: String) {
    XML(".xml"),
    JSON(".json")
}