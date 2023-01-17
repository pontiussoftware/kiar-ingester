package ch.pontius.ingester.parsing.values.primitive

import ch.pontius.ingester.parsing.values.ValueParser

/**
 * A (rather) trivial [ValueParser] implementation that returns a [String].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class StringValueParser: ValueParser<String> {
    private val buffer = StringBuffer()
    override fun parse(value: String) {
        this.buffer.append(value)
    }
    override fun get(): String = this.buffer.toString()
}