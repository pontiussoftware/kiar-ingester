package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
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
 * @version 1.0.1
 */
class JsonFileSource(private val file: Path, private val config: EntityMapping): Source<SolrInputDocument> {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = flow {
        val docParser = JsonDocumentParser(this@JsonFileSource.config, context)
        JsonReader(Files.newBufferedReader(this@JsonFileSource.file)).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                val doc = SolrInputDocument()
                docParser.parse(JsonParser.parseReader(reader), doc)
                emit(doc)
            }
            reader.endArray()
        }
    }.flowOn(Dispatchers.IO)
}