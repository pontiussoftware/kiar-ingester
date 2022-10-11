package ch.pontius.ingester.parsing.values

import java.text.SimpleDateFormat
import java.util.Date

/**
 * [ValueParser] to convert a [String] to a [Date].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DateValueParser: ValueParser<Date> {
    private var buffer: Date? = null
    override fun parse(value: String) {
        this.buffer = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value)
    }
    override fun get() = this.buffer
}