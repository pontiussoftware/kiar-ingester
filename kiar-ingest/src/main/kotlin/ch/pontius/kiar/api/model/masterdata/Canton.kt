package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [Canton].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Canton(val shortName: String, val longName: String)