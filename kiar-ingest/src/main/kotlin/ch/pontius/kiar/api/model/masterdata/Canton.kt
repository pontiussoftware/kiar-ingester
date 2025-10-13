package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [Canton].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
@ConsistentCopyVisibility
data class Canton private constructor(val shortName: String, val longName: String) {
    companion object {
        val DEFAULT = arrayOf(
            Canton("AG", "Aargau"),
            Canton("BE", "Bern"),
            Canton("BL", "Basel-Landschaft"),
            Canton("BS", "Basel-Stadt"),
            Canton("LU", "Luzern"),
            Canton("SO", "Solothurn")
       )
    }
}