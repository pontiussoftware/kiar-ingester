package ch.pontius.kiar.api.model.status

import org.apache.http.client.HttpResponseException


/**
 * An [Exception] that can be translated to an [ErrorStatus].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ErrorStatusException(val code: Int, override val message: String) : HttpResponseException(code, message) {
    fun toStatus() = ErrorStatus(this.code, this.message)
}