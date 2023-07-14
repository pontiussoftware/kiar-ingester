package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_RAW
import ch.pontius.kiar.kiar.KiarFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * A [Source] for a KIAR file, as delivered by mainly smaller museums.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class KiarFileSource(private val file: Path, private val config: EntityMapping, private val skipResources: Boolean = false, private val deleteFileWhenDone: Boolean = true): Source<SolrInputDocument> {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** List of required [AttributeMapping]. */
    private val required: List<AttributeMapping> = this.config.attributes.filter { it.required }

    /**
     * Creates and returns a [Flow] for this [KiarFileSource].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = channelFlow {
        val parser = XmlDocumentParser(this@KiarFileSource.config)

        /* Iterate over Kiar entries. */
        KiarFile(this@KiarFileSource.file).use { kiar ->
            for (e in kiar.iterator()) {
                val doc = e.open().use {
                    parser.parse(it)
                }

                /* Read all resources. */
                if (!this@KiarFileSource.skipResources) {
                    for (i in 0 until e.resources()) {
                        e.openResource(i).use {
                            doc.addField(FIELD_NAME_RAW, ImageIO.read(it))
                        }
                    }
                }

                /* Send document down the channel. */
                if (this@KiarFileSource.validate(doc)) {
                    this.send(doc)
                }
            }
        }
    }.onCompletion {
       try {
           if (this@KiarFileSource.deleteFileWhenDone) {
               Files.deleteIfExists(this@KiarFileSource.file) /* Tries to delete file. */
           }
       } catch (e: Throwable) {
           LOGGER.warn("Failed to delete source KIAR file: ${this@KiarFileSource.file}")
       }
    }.flowOn(Dispatchers.IO)

    /**
     * Internal method that validates a [SolrInputDocument] before sending it downstream for processing.
     *
     * @param doc The [SolrInputDocument] that must be validated.
     * @return True if validation is passed, false otherwise.
     */
    private fun validate(doc: SolrInputDocument): Boolean {
        for (a in this.required) {
            if (!doc.containsKey(a.destination)) {
                LOGGER.warn("Document ${doc[Constants.FIELD_NAME_UUID]} did not pass validation because of missing attribute '${a.destination}'.")
                return false
            }
        }
        return true
    }
}