package ch.pontius.kiar.api.model.config.mappings

import ch.pontius.kiar.database.config.mapping.DbFormat
import kotlinx.serialization.Serializable

/**
 * The types of [EntityMapping]s supported by KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
enum class MappingType {
    XML,   /* XML file based mapping. */
    JSON,  /* JSON file based mapping. */
    EXCEL; /* EXCEL file based mapping. */


    /**
     * Convenience method to convert this [MappingType] to a [DbFormat]. Requires an ongoing transaction!
     *
     * @return [DbFormat]
     */
    fun toDb() = when(this) {
        XML -> DbFormat.XML
        JSON ->  DbFormat.JSON
        EXCEL ->  DbFormat.EXCEL
    }
}