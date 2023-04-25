package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.ingester.parsing.values.ValueParser

/**
 * [ValueParser] to convert a [String] to a [Double].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DoubleValueParser: ValueParser<Double> {
    private var buffer: Double? = null
    override fun parse(value: String) {
        this.buffer = value.toDoubleOrNull()
    }
    override fun get() = this.buffer
}