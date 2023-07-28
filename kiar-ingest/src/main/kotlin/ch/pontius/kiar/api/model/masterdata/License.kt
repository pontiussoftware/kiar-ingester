package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [License] statement.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class License(val short: String, val long: String, val url: String)