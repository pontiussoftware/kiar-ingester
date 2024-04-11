package ch.pontius.kiar.ingester.parsing.values.primitive

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument
import java.lang.IllegalArgumentException
import java.util.UUID

/**
 * [ValueParser] to convert a [String] to a [UUID].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class UuidValueParser(override val mapping: AttributeMapping): ValueParser<String> {
    /**
     * Parses the given [String] into the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    override fun parse(value: String, into: SolrInputDocument) {
        val uuid = try {
            UUID.fromString(value).toString()
        } catch (e: IllegalArgumentException) {
            UUID.randomUUID().toString()
        }
        if (this.mapping.multiValued) {
            into.addField(this.mapping.destination, uuid)
        } else {
            into.setField(this.mapping.destination, uuid)
        }
    }
}