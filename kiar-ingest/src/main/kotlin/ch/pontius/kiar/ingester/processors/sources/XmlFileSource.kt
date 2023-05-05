package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.config.MappingConfig
import ch.pontius.kiar.ingester.parsing.xml.AttributeMapping
import ch.pontius.kiar.ingester.parsing.xml.XmlParsingContext
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * A [Source] for a single XML file. This is, for example, used by culture.web.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class XmlFileSource(override val context: String, private val file: Path, private val config: MappingConfig): Source<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** List of required [AttributeMapping]. */
    private val required: List<AttributeMapping> = this.config.values.filter { it.required }

    /**
     * Creates and returns a [Flow] for this [XmlFileSource].
     *
     * @return [Flow]
     */
    override fun toFlow(): Flow<SolrInputDocument> = channelFlow {
        val channel = this
        val factory: SAXParserFactory = SAXParserFactory.newInstance()
        val saxParser: SAXParser = factory.newSAXParser()
        Files.newInputStream(this@XmlFileSource.file).use { input ->
            val parser = XmlParsingContext(this@XmlFileSource.config) { doc ->
                runBlocking {
                    if (this@XmlFileSource.validate(doc)) {
                        channel.send(doc)
                    }
                }
            }
            saxParser.parse(input, parser)
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
                LOGGER.warn("Document ${doc[FIELD_NAME_UUID]} did not pass validation because of missing attribute '${a.destination}'.")
                return false
            }
        }
        return true
    }
}