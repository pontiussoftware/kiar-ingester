package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument
import java.text.SimpleDateFormat
import java.util.Date

/**
 * [ValueParser] to convert a [String] to a [Date].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DateValueParser(override val mapping: AttributeMapping): ValueParser<Date> {
    /** The date/time format used for parsing the date. */
    private val format = mapping.parameters["format"] ?: "yyyy-MM-dd HH:mm:ss"

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, SimpleDateFormat(this.format).parse(value))
        } else {
            into.setField(this.mapping.destination, SimpleDateFormat(this.format).parse(value))
        }
    }
}