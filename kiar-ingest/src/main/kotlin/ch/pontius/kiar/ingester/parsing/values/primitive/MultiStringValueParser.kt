package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.ingester.parsing.values.ValueParser

/**
 * A (rather) trivial [ValueParser] implementation that returns an array of [Strings].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class MultiStringValueParser(private val separator: String = ", "): ValueParser<List<String>> {
    private val buffer = StringBuffer()
    override fun parse(value: String) {
        this.buffer.append(value)
    }
    override fun get(): List<String> = this.buffer.toString().split(this.separator)
}