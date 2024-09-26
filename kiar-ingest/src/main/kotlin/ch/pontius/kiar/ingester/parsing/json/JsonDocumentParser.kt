package ch.pontius.kiar.ingester.parsing.json

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import com.google.gson.*
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.xml.xpath.XPathExpression

/**
 * A parser for parsing [JsonElement] using JsonPath.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class JsonDocumentParser(private val config: EntityMapping, private val context: ProcessingContext) {

    /** A [Map] of all [XPathExpression]s used for document parsing. */
    private val mappings: List<Pair<ValueParser<*>, String>>

    init {
        Configuration.setDefaults(object : Configuration.Defaults {
            private val jsonProvider: JsonProvider = GsonJsonProvider()
            private val mappingProvider: MappingProvider = GsonMappingProvider()

            override fun jsonProvider(): JsonProvider {
                return this.jsonProvider
            }

            override fun mappingProvider(): MappingProvider {
                return mappingProvider
            }

            override fun options(): Set<Option> {
                return EnumSet.noneOf(Option::class.java)
            }
        })
        this.mappings = this.config.attributes.map { it.newParser() to it.source }
    }

    /**
     * Parses a [Path] pointing to an JSON document into [SolrInputDocument].
     *
     * @param path The [Path] of the file to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(path: Path, into: SolrInputDocument) = Files.newBufferedReader(path).use {
        this.parse(JsonParser.parseReader(it), into)
    }

    /**
     * Parses a [JsonElement] into [SolrInputDocument].
     *
     * @param document The [JsonElement] to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(document: JsonElement, into: SolrInputDocument) {
        for ((parser, source) in this.mappings) {
            try {
                when (val value = JsonPath.read<Any>(document, source)) {
                    is JsonArray -> {
                        for (element in value) {
                            if (element is JsonPrimitive) {
                                parser.parse(element.asString.trim(), into, this.context)
                            }
                        }
                    }
                    is JsonPrimitive -> parser.parse(value.asString.trim(), into, this.context)
                    is JsonNull -> parser.parse(null, into, this.context)
                    else -> throw IllegalArgumentException("Unsupported JSON type: ${value.javaClass}")
                }
            } catch (e: PathNotFoundException) {
                parser.parse(null, into, this.context)
            }
        }
    }
}