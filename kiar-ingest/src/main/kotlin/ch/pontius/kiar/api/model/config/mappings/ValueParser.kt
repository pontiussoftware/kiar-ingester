package ch.pontius.kiar.api.model.config.mappings

import ch.pontius.kiar.database.config.mapping.DbParser
import ch.pontius.kiar.ingester.parsing.values.primitive.*
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
    IMAGE;

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
        IMAGE -> DbParser.IMAGE
    }

    /**
     * Returns a new [ValueParser] instance for this [ValueParser] value.
     *
     * @return [ValueParser].
     */
    fun newInstance(params: Map<String,String> = emptyMap()) = when (this) {
        UUID -> UuidValueParser()
        STRING -> StringValueParser()
        MULTISTRING -> MultiStringValueParser()
        INTEGER -> IntegerValueParser()
        DOUBLE -> DoubleValueParser()
        DATE -> DateValueParser(params)
        IMAGE -> ImageValueParser(params)
    }
}