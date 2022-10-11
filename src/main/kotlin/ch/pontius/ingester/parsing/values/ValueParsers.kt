package ch.pontius.ingester.parsing.values

import ch.pontius.ingester.parsing.values.primitive.*
import ch.pontius.ingester.parsing.values.system.CantonParser

/**
 * An enumeration of all [ValueParsers] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ValueParsers {
    STRING,
    DATE,
    INTEGER,
    DOUBLE,
    IMAGE,
    CANTON;

    /**
     * Returns a new [ValueParser] instance for this [ValueParsers] value.
     *
     * @return [ValueParser].
     */
    fun newInstance(params: Map<String,String> = emptyMap()) = when (this) {
        STRING -> StringValueParser()
        INTEGER -> IntegerValueParser()
        DOUBLE -> DoubleValueParser()
        DATE -> DateValueParser(params)
        IMAGE -> ImageValueParser(params)
        CANTON -> CantonParser()
    }
}