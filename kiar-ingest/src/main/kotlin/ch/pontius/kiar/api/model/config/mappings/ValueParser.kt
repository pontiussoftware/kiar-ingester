package ch.pontius.kiar.api.model.config.mappings

import ch.pontius.kiar.database.config.mapping.DbParser
import kotlinx.serialization.Serializable

/**
 * An enumeration of all [ValueParser] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
@Serializable
enum class ValueParser {
    UUID,
    STRING,
    MULTISTRING,
    DATE,
    INTEGER,
    DOUBLE,
    COORD_WGS84,
    COORD_LV95,
    IMAGE_FILE,
    IMAGE_URL,
    IMAGE_MPLUS;


    /**
     * Converts this [ValueParser] into a [DbParser]. Requires an ongoing transaction.
     *
     * @return [DbParser].
     */
    fun toDb(): DbParser = when(this) {
        UUID -> DbParser.UUID
        STRING -> DbParser.STRING
        MULTISTRING -> DbParser.MULTISTRING
        DATE -> DbParser.DATE
        INTEGER -> DbParser.INTEGER
        DOUBLE -> DbParser.DOUBLE
        COORD_WGS84 -> DbParser.COORD_WGS84
        COORD_LV95 -> DbParser.COORD_LV95
        IMAGE_FILE -> DbParser.IMAGE_FILE
        IMAGE_URL -> DbParser.IMAGE_URL
        IMAGE_MPLUS -> DbParser.IMAGE_MPLUS
    }
}