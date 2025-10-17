package ch.pontius.kiar.api.model.config.mappings

import kotlinx.serialization.Serializable

/**
 * An enumeration of all [ValueParser] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.1.0
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
}