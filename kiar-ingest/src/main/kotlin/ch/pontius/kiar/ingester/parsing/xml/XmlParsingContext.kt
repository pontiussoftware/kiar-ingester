package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.processors.ProcessingContext
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.solr.common.SolrInputDocument
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.DocumentBuilder

/** The [KLogger] instance for [XmlParsingContext]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A [XmlParsingContext] used to parse and map XML files to [SolrInputDocument]s.
 *
 * Processing of the individual [SolrInputDocument] is provided by a callback method.
 *
 * @author Ralph Gasser
 * @version 1.2.4
 */
class XmlParsingContext(config: EntityMapping, private val context: ProcessingContext, private val callback: (SolrInputDocument) -> Unit): DefaultHandler() {
    /** Internal [StringBuffer] used for buffering raw characters during XML parsing. */
    private var buffer = StringBuffer()

    /** The current XPath this [XmlParsingContext] is currently in. */
    private var xpath = ""

    /** The longest, common prefix found for all [AttributeMapping]. This prefix will be used to distinguish between different objects. */
    private val newDocumentOn: String

    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = XmlFactories.newDocumentBuilderFactory().newDocumentBuilder()

    /** The [XmlDocumentParser] to use for parsing individual documents. */
    private val parser: XmlDocumentParser

    /** The currently active XML [Node] (to which new [Element]s will be appended). */
    private var appendTo: Node = this.documentBuilder.newDocument()

    /** The [SAXParseException] encountered while parsing the current document; null if no error occurred. */
    private var error: SAXParseException? = null

    init {
        /* Determine common prefix. */
        var commonPrefix = config.attributes.first().source
        config.attributes.forEach {
            commonPrefix = commonPrefix.commonPrefixWith(it.source)
        }
        val prefixArray = commonPrefix.split('/')
        this.newDocumentOn = prefixArray.subList(0, prefixArray.size - 2).joinToString("/")

        /* Copy parser and adjust attributes source parameter. */
        this.parser = XmlDocumentParser(config.copy(attributes = config.attributes.map { it.copy( source = "/${it.source.replace(this.newDocumentOn, "")}") }), this.context)
    }

    /**
     * Adds the new element to the stack.
     */
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        this.xpath += "/$qName"

        if (this.xpath.length > this.newDocumentOn.length) {
            /* Create new XML element and append to current one. */
            this.appendTo = newElement(this.appendTo, qName, attributes)
        }
    }

    /**
     * Removes the new element from the stack
     */
    override fun endElement(uri: String, localName: String, qName: String) {
        /* Pop stack. */
        this.xpath = this.xpath.removeSuffix("/$qName")

        /* Flush old document (if needed). */
        if (this.xpath == this.newDocumentOn) {
            val error = this.error
            if (error != null) {
                this.context.log(JobLog(this.context.jobId, null, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Skipped document due to XML parse error at line ${error.lineNumber}, column ${error.columnNumber}: ${error.message}"))
                this.error = null /* Clear error when new document starts. */
            } else {
                val doc = SolrInputDocument()
                this.parser.parse(this.appendTo.ownerDocument, doc)
                this.callback(doc)
            }

            /* Create new XML document. */
            this.appendTo = this.documentBuilder.newDocument()
        } else {
            if (this.buffer.isNotBlank()) {
                this.appendTo.textContent = this.buffer.toString().trim()
                this.buffer = StringBuffer()
            }
            if (this.appendTo.parentNode != null) {
                this.appendTo = this.appendTo.parentNode
            }
        }
    }

    /**
     * Processes the characters read from an element.
     *
     * @param ch The [CharArray] to read from.
     * @param start The start position to read.
     * @param length The length of the string to read.
     */
    override fun characters(ch: CharArray, start: Int, length: Int) {
        this.buffer.append(ch.copyOfRange(start, start + length))
    }

    /**
     * Handles [SAXParseException].
     *
     * @param e [SAXParseException]
     */
    override fun error(e: SAXParseException) {
        logger.error(e) { "SAX parse error encountered while parsing document: ${e.message}." }
        if (this.error == null) this.error = e
    }

    /**
     * Handles fatal [SAXParseException].
     *
     * @param e [SAXParseException]
     */
    override fun fatalError(e: SAXParseException) {
        logger.error(e) {  "Fatal SAX parse error encountered while parsing document: ${e.message}." }
        if (this.error == null) this.error = e
    }

    /**
     * Creates a new [Element] in the given [Document].
     *
     * @param node The [Node] to append [Element] to.
     * @param qName The qualified name of the [Element].
     * @param attributes The [Attributes] of the [Element].
     */
    private fun newElement(node: Node, qName: String, attributes: Attributes): Element {
        val element = if (node is Document) {
            node.createElement(qName)
        } else{
            node.ownerDocument.createElement(qName)
        }
        for (i in 0 until attributes.length) {
            element?.setAttribute(attributes.getQName(i), attributes.getValue(i))
        }
        node.appendChild(element)
        return element
    }
}