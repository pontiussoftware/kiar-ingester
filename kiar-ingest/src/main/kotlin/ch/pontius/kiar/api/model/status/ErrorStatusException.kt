package ch.pontius.kiar.api.model.status


/**
 * An [Exception] that can be translated to an [ErrorStatus].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ErrorStatusException(val code: Int, override val message: String) : Exception(message) {
    fun toStatus() = ErrorStatus(this.code, this.message)
}