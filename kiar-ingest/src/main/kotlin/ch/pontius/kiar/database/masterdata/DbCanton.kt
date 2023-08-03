package ch.pontius.kiar.database.masterdata

import ch.pontius.kiar.api.model.masterdata.Canton
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of all [DbCanton]s available to KIM.ch Data Ingest Platform.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbCanton(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbCanton>() {
        val AG by enumField {
            short = "AG"
            long = "Aargau"
        }
        val BL by enumField {
            short = "BL"
            long = "Basel-Landschaft"
        }
        val BS by enumField {
            short = "BS";
            long = "Basel-Stadt"
        }
        val BE by enumField {
            short = "BE"
            long = "Bern"
        }
        val LU by enumField {
            short = "LU"
            long = "Luzern"
        }
        val SO by enumField {
            short = "SO"
            long = "Solothurn"
        }
    }

    /** The short name / description of this [DbCanton]. */
    var short by xdRequiredStringProp(unique = true)

    /** The long name / description of this [DbCanton]. */
    var long by xdRequiredStringProp(unique = true)

    /**
     * Convenience method to convert this [DbCanton] to a [Canton].
     *
     * Requires an ongoing transaction.
     *
     * @return [Canton]
     */
    fun toApi() = Canton(this.short, this.long)
}