package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.xml.XmlParsingContext
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.setField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
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
class XmlFileSource(private val file: Path, private val config: EntityMapping): Source<SolrInputDocument> {
    /**
     * Creates and returns a [Flow] for this [XmlFileSource].
     *
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = channelFlow {
        val channel = this
        val factory: SAXParserFactory = SAXParserFactory.newInstance()
        val saxParser: SAXParser = factory.newSAXParser()
        Files.newInputStream(this@XmlFileSource.file).use { input ->
            val parser = XmlParsingContext(this@XmlFileSource.config, context) { doc ->
                runBlocking {
                    doc.setField(Field.PARTICIPANT, context.participant)
                    channel.send(doc)
                }
            }
            saxParser.parse(input, parser)
        }
    }.flowOn(Dispatchers.IO)
}