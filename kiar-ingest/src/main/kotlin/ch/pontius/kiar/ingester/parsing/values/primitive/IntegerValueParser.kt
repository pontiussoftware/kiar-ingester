package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuid
import org.apache.solr.common.SolrInputDocument

/**
 * [ValueParser] to convert a [String] to a [Int].
 *
 * @author Ralph Gasser
 * @version 2.1.0
 */
class IntegerValueParser(override val mapping: AttributeMapping): ValueParser<Int> {
    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     *
     */
    override fun parse(value: String, into: SolrInputDocument, context: ProcessingContext) {
        val parsedValue = try {
            value.toInt()
        } catch (e: NumberFormatException) {
            context.log(JobLog(context.jobId, into.uuid(), null, JobLogContext.METADATA, JobLogLevel.WARNING, "Failed to parse integer '$value' for field ${this.mapping.destination}."))
            return
        }
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, parsedValue)
        } else {
            into.setField(this.mapping.destination, parsedValue)
        }
    }
}