package ch.pontius.kiar.api.model.config.mappings

import ch.pontius.kiar.database.config.mapping.DbParser
import kotlinx.serialization.Serializable

/**
 * An enumeration of all [ValueParser] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
enum class ValueParser {
    UUID,
    STRING,
    MULTISTRING,
    DATE,
    INTEGER,
    DOUBLE,
    IMAGE_FILE,
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
        IMAGE_FILE -> DbParser.IMAGE_FILE
        IMAGE_MPLUS -> DbParser.IMAGE_MPLUS
    }
}