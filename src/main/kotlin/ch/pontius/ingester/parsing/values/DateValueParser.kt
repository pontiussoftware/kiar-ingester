package ch.pontius.ingester.parsing.values

import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.Date

/**
 * [ValueParser] to convert a [String] to a [Date].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DateValueParser(params: Map<String,String>): ValueParser<Date> {
    /** The last [Date] extracted by this [DateValueParser]. */
    private var buffer: Date? = null

    /** The date/time format used for parsing the date. */
    private val format = params["format"] ?: "yyyy-MM-dd HH:mm:ss"

    override fun parse(value: String) {
        this.buffer = SimpleDateFormat(this.format).parse(value)
    }
    override fun get() = this.buffer
}