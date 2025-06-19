package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import org.apache.solr.common.SolrInputDocument
import java.lang.IllegalArgumentException
import java.util.UUID

/**
 * [ValueParser] to convert a [String] to a [UUID].
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class UuidValueParser(override val mapping: AttributeMapping): ValueParser<UUID> {
    /**
     * Parses the given [String] as an [UUID] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse or null.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return
        val uuid = try {
            UUID.fromString(value).toString()
        } catch (e: IllegalArgumentException) {
            context.log(JobLog(context.jobId, into.uuidOrNull(), null, JobLogContext.METADATA, JobLogLevel.WARNING, "Failed to parse UUID '$value' for field ${this.mapping.destination}."))
            return
        }
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, uuid)
        } else {
            into.setField(this.mapping.destination, uuid)
        }
    }
}