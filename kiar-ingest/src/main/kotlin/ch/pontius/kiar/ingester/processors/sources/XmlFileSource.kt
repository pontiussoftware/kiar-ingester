package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.xml.XmlParsingContext
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.transformers.InstitutionTransformer
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.setField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import org.xml.sax.SAXException
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * A [Source] for a single XML file. This is, for example, used by culture.web.
 *
 * @author Ralph Gasser
 * @version 1.1.1
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
                    if (context.aborted) throw InterruptedException("XML parsing was aborted by user.")
                    channel.send(doc)
                }
            }

            /*
             * This is a special error handling construct; SAX parser must be interrupted manually by throwing an exception.
             * Otherwise, parsing won't stop. However, the parser wraps the exception we throw. Hence, we need to make
             * this check for the InterruptedException.
             */
            try {
                saxParser.parse(input, parser)
            } catch (e: SAXException) {
                if (e.cause !is InterruptedException) {
                    throw e
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}