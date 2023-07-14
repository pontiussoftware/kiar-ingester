package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import org.apache.solr.common.SolrInputDocument
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

/**
 * A parser for parsing simple XML [Node]s and documents into [SolrInputDocument]s using an [EntityMapping].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class XmlDocumentParser(private val config: EntityMapping) {

    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** The [XPath] instance used by this [XmlDocumentParser]. */
    private val xPath: XPath = XPathFactory.newInstance().newXPath()

    /** A [Map] of all [XPathExpression]s used for document parsing. */
    private val xpaths = this.config.attributes.associateWith { this.xPath.compile(it.source) }

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
        return parse(xmlDocument)
    }

    /**
     * Parses a XML [Node] into [SolrInputDocument].
     *
     * @param node The [Node] to parse.
     * @return [SolrInputDocument]
     */
    fun parse(node: Node): SolrInputDocument {
        val solrDocument = SolrInputDocument()
        for (mapping in this.config.attributes) {
            val expr = this.xpaths[mapping]
            val nl = expr?.evaluate(node, XPathConstants.NODESET) as? NodeList
            if (nl != null) {
                for (i in 0 until nl.length) {
                    val parser = mapping.parser.newInstance(mapping.parameters)
                    val value = nl.item(i).nodeValue
                    if (!value.isNullOrBlank()) {
                        parser.parse(nl.item(i).nodeValue)
                        if (mapping.multiValued) {
                            solrDocument.addField(mapping.destination, parser.get())
                        } else {
                            solrDocument.setField(mapping.destination, parser.get())
                        }
                    }
                }
            }
        }
        return solrDocument
    }
}