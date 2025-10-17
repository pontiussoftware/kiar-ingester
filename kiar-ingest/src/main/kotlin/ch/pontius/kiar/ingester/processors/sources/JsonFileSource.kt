package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.ingester.parsing.json.JsonDocumentParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path

/**
 * A [Source] for a JSON file, as delivered by mainly smaller museums.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class JsonFileSource(private val file: Path): Source<SolrInputDocument> {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = flow {
        val mapping = context.jobTemplate.mapping ?: throw IllegalArgumentException("No entity mapping for job with ID ${context.jobId} found.")
        val docParser = JsonDocumentParser(mapping, context)
        JsonReader(Files.newBufferedReader(this@JsonFileSource.file)).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                /* Parse document. */
                val doc = SolrInputDocument()
                docParser.parse(JsonParser.parseReader(reader), doc)

                /* Check if context is still active. Break otherwise. */
                if (context.aborted) break

                /* Emit document. */
                emit(doc)
            }
            reader.endArray()
        }
    }.flowOn(Dispatchers.IO)
}