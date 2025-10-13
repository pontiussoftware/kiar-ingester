package ch.pontius.kiar.migration.database.masterdata

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of all [DbRightStatement]s available to KIM.ch Data Ingest Platform.
 * 
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbRightStatement(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbRightStatement>() {
        val InC by enumField {
            short = "InC"
            long = "In Copyright - Re-use Not Permitted"
            url = "https://rightsstatements.org/vocab/InC/1.0/"
        }
        val InC_EDU by enumField {
            short = "InC-EDU"
            long = "In Copyright - Educational Use Permitted"
            url = "https://rightsstatements.org/vocab/InC-EDU/1.0/"
        }
        val CNE by enumField {
            short = "CNE"
            long = "Copyright Not Evaluated"
            url = "https://rightsstatements.org/vocab/CNE/1.0/"
        }
        val CC_0 by enumField {
            short = "CC0"
            long = "The Creative Commons CC0 1.0 Universal Public Domain Dedication"
            url = "https://creativecommons.org/publicdomain/mark/1.0/"
        }
        val PDM by enumField {
            short = "PDM"
            long = "The Public Domain Mark (PDM)"
            url = "https://creativecommons.org/publicdomain/zero/1.0/"
        }
        val CC_BY by enumField {
            short = "CC BY 4.0"
            long = "Creative Commons - Attribution"
            url = "https://creativecommons.org/licenses/by/4.0/"
        }
        val CC_BY_SA by enumField {
            short = "CC BY-SA 4.0"
            long = "Creative Commons - Attribution, ShareAlike"
            url = "https://creativecommons.org/licenses/by-sa/4.0/"
        }
        val CC_BY_NC by enumField {
            short = "CC BY-NC 4.0"
            long = "Creative Commons - Attribution, Non-Commercial"
            url = "https://creativecommons.org/licenses/by-nc/4.0/"
        }
        val CC_BY_ND by enumField {
            short = "CC BY-ND 4.0"
            long = "Creative Commons - Attribution, No Derivatives"
            url = "https://creativecommons.org/licenses/by-nd/4.0/"
        }
        val CC_BY_NC_SA by enumField {
            short = "CC BY-NC-SA 4.0"
            long = "Creative Commons - Attribution, Non-Commercial, ShareAlike"
            url = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
        }
        val CC_BY_NC_ND by enumField {
            short = "CC BY-NC-ND 4.0"
            long = "Creative Commons - Attribution, Non-Commercial, No Derivatives"
            url = "https://creativecommons.org/licenses/by-nc-nd/4.0/"
        }
    }

    /** The name / description of this [DbRightStatement]. */
    var short by xdRequiredStringProp(unique = true)

    /** The name / description of this [DbRightStatement]. */
    var long by xdRequiredStringProp(unique = true)

    /** The name / description of this [DbRightStatement]. */
    var url by xdRequiredStringProp(unique = true)
}