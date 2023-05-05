package ch.pontius.kiar.api.model.status

import kotlinx.serialization.Serializable

/**
 * A [SuccessStatus] as returned by the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class SuccessStatus(val description: String)