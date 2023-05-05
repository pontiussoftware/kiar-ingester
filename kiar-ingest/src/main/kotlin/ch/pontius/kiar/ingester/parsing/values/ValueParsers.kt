package ch.pontius.kiar.ingester.parsing.values

import ch.pontius.kiar.DB
import ch.pontius.kiar.database.config.mapping.DbParser
import ch.pontius.kiar.database.config.transformers.DbTransformerType
import ch.pontius.kiar.ingester.parsing.values.primitive.*
import ch.pontius.kiar.ingester.processors.transformers.Transformers

/**
 * An enumeration of all [ValueParsers] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ValueParsers {
    UUID,
    STRING,
    MULTISTRING,
    DATE,
    INTEGER,
    DOUBLE,
    IMAGE;

    /**
     * Converts this [ValueParsers] into a [DbParser]. Requires an ongoing transaction.
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
     * Returns a new [ValueParser] instance for this [ValueParsers] value.
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