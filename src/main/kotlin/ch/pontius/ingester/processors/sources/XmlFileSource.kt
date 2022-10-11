package ch.pontius.ingester.processors.sources

import ch.pontius.ingester.config.MappingConfig
import ch.pontius.ingester.parsing.xml.XmlParsingContext
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * A [Source] for a single XML file. This is, for example, used by culture.web.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class XmlFileSource(private val file: Path, private val config: MappingConfig): Source<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

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
                if (this@XmlFileSource.validate(doc)) {
                    runBlocking {
                        channel.send(doc)
                    }
                }
            }
            saxParser.parse(input, parser)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Validates a [SolrDocument] with respect to the [MappingConfig].
     *
     * @param doc The [SolrDocument] to validate.
     * @return True if validation passes, false otherwise.
     */
    private fun validate(doc: SolrInputDocument): Boolean {
        for (m in this.config.values.filter { it.required }) {
            if (!doc.containsKey(m.destination)) {
                LOGGER.warn("Failed to validate document ${doc[FIELD_NAME_UUID]}; skipping...")
                return false
            }
        }
        return true
    }
}