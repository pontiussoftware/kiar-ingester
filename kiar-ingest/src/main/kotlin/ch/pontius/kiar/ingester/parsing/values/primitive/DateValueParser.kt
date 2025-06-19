package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import org.apache.solr.common.SolrInputDocument
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * [ValueParser] to convert a [String] to a [Date].
 *
 * @author Ralph Gasser
 * @version 2.2.1
 */
class DateValueParser(override val mapping: AttributeMapping): ValueParser<Date> {
    /** The date/time format used for parsing the date. */
    private val format = SimpleDateFormat(this.mapping.parameters["format"] ?: "yyyy-MM-dd HH:mm:ss")

    /** Default value that should be used if source value has not been specified. */
    private val default: Date? = this.mapping.parameters["default"]?.let { this.format.parse(it) }

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        /* Handle null value and set default. */
        if (value.isNullOrEmpty()) {
            if (this.default != null) {
                into.setField(this.mapping.destination, this.default)
            }
            return
        }

        /* Parse value. */
        val parsed = try {
            this.format.parse(value)
        } catch (e: ParseException) {
            context.log(JobLog(context.jobId, into.uuidOrNull(), null, JobLogContext.METADATA, JobLogLevel.WARNING, "Failed to parse date '$value' for field ${this.mapping.destination} using format '${this.format.toPattern()}'."))
        }
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, parsed)
        } else {
            into.setField(this.mapping.destination, parsed)
        }
    }
}