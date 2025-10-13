package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.addField
import ch.pontius.kiar.ingester.solrj.setField
import ch.pontius.kiar.kiar.KiarFile
import com.sksamuel.scrimage.ImmutableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
import java.nio.file.Path

/**
 * A [Source] for a KIAR file, as delivered by mainly smaller museums.
 *
 * @author Ralph Gasser
 * @version 1.1.2
 */
class KiarFileSource(private val file: Path, private val config: EntityMapping, private val skipResources: Boolean = false): Source<SolrInputDocument> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /**
     * Creates and returns a [Flow] for this [KiarFileSource].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        val kiar = KiarFile(this@KiarFileSource.file)
        return channelFlow {
            val parser = XmlDocumentParser(this@KiarFileSource.config, context)

            /* Iterate over Kiar entries. */
            for (entry in kiar.iterator()) {
                /* Create new document. */
                val doc = SolrInputDocument()
                doc.setField(Field.UUID, entry.uuid.toString())
                doc.setField(Field.PARTICIPANT, context.participant)

                /* Parse values. */
                entry.open().use {
                    parser.parse(it, doc)
                }

                /* Read all resources. */
                if (!this@KiarFileSource.skipResources) {
                    for (i in 0 until entry.resources()) {
                        doc.addField(Field.RAW, KiarImageProvider(i, entry))
                    }
                }

                /* Check if context is still active. Break otherwise. */
                if (context.aborted) break

                /* Send document down the channel. */
                this.send(doc)
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * A [MediaProvider.Image] for the images contained in this [KiarFileSource].
     */
    private data class KiarImageProvider(private val index: Int, private val entry: KiarFile.KiarEntry): MediaProvider.Image {
        override fun open(): ImmutableImage? = try {
            this.entry.openResource(this.index).use {
                ImmutableImage.loader().fromStream(it)
            }
        } catch (e: IOException) {
            LOGGER.warn("Failed to decode image ${this.index} from KIAR entry ${entry.uuid}. An exception occurred: ${e.message}")
            null
        }
    }
}