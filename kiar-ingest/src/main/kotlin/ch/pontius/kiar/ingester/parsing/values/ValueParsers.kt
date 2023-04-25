package ch.pontius.kiar.ingester.parsing.values

import ch.pontius.kiar.ingester.parsing.values.primitive.*

/**
 * An enumeration of all [ValueParsers] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ValueParsers {
    UUID,
    STRING,
    DATE,
    INTEGER,
    DOUBLE,
    IMAGE;

    /**
     * Returns a new [ValueParser] instance for this [ValueParsers] value.
     *
     * @return [ValueParser].
     */
    fun newInstance(params: Map<String,String> = emptyMap()) = when (this) {
        UUID -> UuidValueParser()
        STRING -> StringValueParser()
        INTEGER -> IntegerValueParser()
        DOUBLE -> DoubleValueParser()
        DATE -> DateValueParser(params)
        IMAGE -> ImageValueParser(params)
    }
}