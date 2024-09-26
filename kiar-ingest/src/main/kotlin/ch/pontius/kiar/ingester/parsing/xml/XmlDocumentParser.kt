package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import org.apache.solr.common.SolrInputDocument
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

/**
 * A parser for parsing simple XML [Node]s and documents into [SolrInputDocument]s using an [EntityMapping].
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
class XmlDocumentParser(config: EntityMapping, private val context: ProcessingContext) {
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
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(path: Path, into: SolrInputDocument) = Files.newInputStream(path, StandardOpenOption.READ).use {
        this.parse(it, into)
    }

    /**
     * Parses a simple [InputStream] representing an XML document into [SolrInputDocument].
     *
     * @param stream The [InputStream] to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(stream: InputStream, into: SolrInputDocument) {
        val xmlDocument = this.documentBuilder.parse(stream)
        this.parse(xmlDocument, into)
    }

    /**
     * Parses a XML [Node] into [SolrInputDocument].
     *
     * @param node The [Node] to parse.
     * @param into The [SolrInputDocument] to append the value to.
     */
    fun parse(node: Node, into: SolrInputDocument) {
        for ((parser, expr) in this.mappings) {
            val nl = expr.evaluate(node, XPathConstants.NODESET) as? NodeList
            if (nl != null) {
                for (i in 0 until nl.length) {
                    parser.parse(nl.item(i).nodeValue, into, context)
                }
            }
        }
    }
}