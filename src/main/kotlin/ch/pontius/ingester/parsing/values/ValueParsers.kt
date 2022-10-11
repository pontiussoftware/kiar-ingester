package ch.pontius.ingester.parsing.values

/**
 * An enumeration of all [ValueParsers] supported by Ingester.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ValueParsers {
    STRING, DATE, INTEGER, DOUBLE, IMAGE;

    /**
     * Returns a new [ValueParser] instance for this [ValueParsers] value.
     *
     * @return [ValueParser].
     */
    fun newInstance() = when (this) {
        STRING -> StringValueParser()
        INTEGER -> IntegerValueParser()
        DOUBLE -> DoubleValueParser()
        DATE -> DateValueParser()
        IMAGE -> ImageValueParser()
    }
}