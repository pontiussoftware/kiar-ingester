package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import org.apache.solr.common.SolrInputDocument

/**
 * [ValueParser] to convert a [String] to a [Int].
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class IntegerValueParser(override val mapping: AttributeMapping): ValueParser<Int> {

    /** Default value that should be used if source value has not been specified. */
    private val default: Int? = this.mapping.parameters["default"]?.toIntOrNull()

    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse or null.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     *
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
        try {
            val parsedValue = value.toInt()
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, parsedValue)
            } else {
                into.setField(this.mapping.destination, parsedValue)
            }
        } catch (e: NumberFormatException) {
            context.log(JobLog(context.jobId, into.uuidOrNull(), null, JobLogContext.METADATA, JobLogLevel.WARNING, "Failed to parse integer '$value' for field ${this.mapping.destination}."))
            return
        }
    }
}