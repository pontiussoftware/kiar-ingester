package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.setField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.apache.solr.common.SolrInputDocument
import java.security.MessageDigest

/**
 * [Transformer] that generates a hash based UUID for a [SolrInputDocument].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class HashUuidGenerator(override val input: Source<SolrInputDocument>, private val parameters: Map<String, String>): Transformer<SolrInputDocument, SolrInputDocument> {

    /** The fields that should be used to generate the has. */
    private val fields = this.parameters["fields"]?.split(",")?.map { it.trim() } ?: emptyList()

    /**
     * Converts this [HashUuidGenerator] to a [Flow]
     *
     * @param context The [ProcessingContext] for the [Flow]
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        val md = MessageDigest.getInstance("MD5")
        return this.input.toFlow(context).onEach { doc ->
            if (doc.get<String>(Field.UUID) == null) {
                val builder = StringBuilder()
                for (field in fields) {
                    builder.append(doc[field])
                }
                val byteArray = builder.toString().toByteArray()
                val hashBytes = md.digest(byteArray)
                doc.setField(Field.UUID, hashBytes.joinToString("") { "%02x".format(it) }.let {
                    "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20)}"
                })            }
        }
    }
}