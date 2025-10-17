package ch.pontius.kiar.api.model.masterdata

import kotlinx.serialization.Serializable

/**
 * A representation of a [RightStatement] statement.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
@ConsistentCopyVisibility
data class RightStatement private constructor(val shortName: String, val longName: String, val url: String) {
    companion object {
        val DEFAULT = arrayOf(
            RightStatement("InC", "In Copyright - Re-use Not Permitted", "https://rightsstatements.org/vocab/InC/1.0/"),
            RightStatement("InC-EDU", "In Copyright - Educational Use Permitted", "https://rightsstatements.org/vocab/InC-EDU/1.0/"),
            RightStatement("CNE", "Copyright Not Evaluated", "https://rightsstatements.org/vocab/CNE/1.0/"),
            RightStatement("CC0", "The Creative Commons CC0 1.0 Universal Public Domain Dedication", "https://creativecommons.org/publicdomain/mark/1.0/"),
            RightStatement("PDM", "The Public Domain Mark (PDM)", "https://creativecommons.org/publicdomain/zero/1.0/"),
            RightStatement("InC", "In Copyright - Re-use Not Permitted", "https://rightsstatements.org/vocab/InC/1.0/"),
            RightStatement("CC BY 4.0", "Creative Commons - Attribution", "https://creativecommons.org/licenses/by/4.0/"),
            RightStatement("CC BY-SA 4.0", "Creative Commons - Attribution, ShareAlike", "https://creativecommons.org/licenses/by-sa/4.0/"),
            RightStatement("CC BY-NC 4.0", "Creative Commons - Attribution, Non-Commercial", "https://creativecommons.org/licenses/by-nc/4.0/"),
            RightStatement("CC BY-ND 4.0", "Creative Commons - Attribution, No Derivatives", "https://creativecommons.org/licenses/by-nd/4.0/"),
            RightStatement("CC BY-NC-SA 4.0", "Creative Commons - Attribution, Non-Commercial, ShareAlike", "https://creativecommons.org/licenses/by-nd/4.0/"),
            RightStatement("CC BY-NC-ND 4.0", "Creative Commons - Attribution, Non-Commercial, No Derivatives", "https://creativecommons.org/licenses/by-nc-nd/4.0/")
        )
    }
}