package ch.pontius.ingester.parsing.values

/**
 * [ValueParser] to convert a [String] to a [Int].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class IntegerValueParser: ValueParser<Int> {
    private var buffer: Int? = null
    override fun parse(value: String) {
        this.buffer = value.toIntOrNull()
    }
    override fun get() = this.buffer
}