package ch.pontius.kiar.config

import kotlinx.serialization.Serializable

/**
 * Configuration to connect to a imdas pro database.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ImdasConfig(val host: String = "127.0.0.1", val port: Int = 5432, val username: String, val password: String, val database: String = "imdas") {
    fun toURL() = "jdbc:postgresql://${this.host}:${this.port}/${database}?user=${this.username}&password=${this.password}&ssl=true"
}