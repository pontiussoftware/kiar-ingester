package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [Canton].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
enum class Canton(val longName: String) {
    AG("Aargau"),
    BE("Bern"),
    BL("Basel-Landschaft"),
    BS("Basel-Stadt"),
    LU("Luzern"),
    SO("Solothurn")
}