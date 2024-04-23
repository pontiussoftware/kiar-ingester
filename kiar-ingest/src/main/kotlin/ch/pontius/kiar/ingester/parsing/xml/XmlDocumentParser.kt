package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import org.apache.solr.common.SolrInputDocument
import org.w3c.dom.Node
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

/**
 * A parser for parsing simple XML [Node]s and documents into [SolrInputDocument]s using an [EntityMapping].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class XmlDocumentParser(config: EntityMapping) {
    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** A [Map] of all [XPathExpression]s used for document parsing. */
    private val mappings: List<Pair<ValueParser<*>,XPathExpression>>

    init {
        val factory = XPathFactory.newInstance().newXPath()
        this.mappings = config.attributes.map { it.newParser() to factory.compile(it.source) }
    }

    /**
     * Parses a [Path] pointing to an XML document into [SolrInputDocument].
     *
     * @param path The [Path] of the file to parse.
     * @return [SolrInputDocument]
     */
    fun parse(path: Path): SolrInputDocument = Files.newInputStream(path, StandardOpenOption.READ).use {
        return parse(it)
    }

    /**
     * Parses a simple [InputStream] representing an XML document into [SolrInputDocument].
     *
     * @param stream The [InputStream] to parse.
     * @return [SolrInputDocument]
     */
    fun parse(stream: InputStream): SolrInputDocument {
        val xmlDocument = this.documentBuilder.parse(stream)
        return this.mappings.parse(xmlDocument)
    }
}