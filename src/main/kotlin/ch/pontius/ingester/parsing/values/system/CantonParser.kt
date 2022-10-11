package ch.pontius.ingester.parsing.values.system

import ch.pontius.ingester.parsing.values.ValueParser

/**
 * Parses the canton.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CantonParser: ValueParser<String> {
    private var buffer: String? = null
    override fun parse(value: String) {
        this.buffer = value.substring(0..1).uppercase()
    }
    override fun get(): String = this.buffer.toString()
}