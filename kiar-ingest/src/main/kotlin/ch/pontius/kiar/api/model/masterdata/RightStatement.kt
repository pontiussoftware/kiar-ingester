package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [RightStatement] statement.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class RightStatement(val shortName: String, val longName: String, val url: String)