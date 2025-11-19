package ch.pontius.kiar.api.model.masterdata

/**
 * A representation of a [Canton].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
enum class Canton(val shortName: String, val longName: String) {
    AG("AG", "Aargau"),
    BE("BE", "Bern"),
    BL("BL","Basel-Landschaft"),
    BS("BS","Basel-Stadt"),
    LU("LU","Luzern"),
    SO("SO","Solothurn")
}