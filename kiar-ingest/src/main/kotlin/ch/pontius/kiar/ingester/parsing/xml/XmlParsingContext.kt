package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.solrj.Constants
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.util.*

/**
 * A [XmlParsingContext] used to parse and map XML files to [SolrInputDocument]s.
 *
 * Processing of the individual [SolrInputDocument] is provided by a callback method.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class XmlParsingContext(config: EntityMapping, val callback: (SolrInputDocument) -> Unit): DefaultHandler() {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The current [SolrInputDocument] that is being processed. */
    private var document = SolrInputDocument()

    /** Internal [StringBuffer] used for buffering raw characters during XML parsing. */
    private var buffer = StringBuffer()

    /** The current XPath this [XmlParsingContext] is currently in. */
    private var xpath = "/"

    /** The current XPath this [XmlParsingContext] is currently in. */
    private val stack = Stack<String>()

    /** The longest, common prefix found for all [AttributeMapping]. This prefix will be used to distinguish between different objects. */
    private val mappings = HashMap<String, MutableList<ValueParser<*>>>()

    /** The longest, common prefix found for all [AttributeMapping]. This prefix will be used to distinguish between different objects. */
    private val newDocumentOn: String

    /** An internal error flag */
    private var error: Boolean = false

    init {
        var commonPrefix = config.attributes.first().source
        config.attributes.forEach {
            this.mappings.compute(it.source) { k, v ->
                var list = v
                if (list == null) {
                    list = mutableListOf()
                }
                list.add(it.newParser())
                list
            }
            commonPrefix = commonPrefix.commonPrefixWith(it.source)
        }
        val prefixArray = commonPrefix.split('/')
        this.newDocumentOn = prefixArray.subList(0, prefixArray.size - 2).joinToString("/")
    }

    /**
     * Adds the new element to the stack.
     */
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        this.stack.add(qName)
        this.xpath = "/${this.stack.joinToString("/")}"
    }

    /**
     * Removes the new element from the stack
     */
    override fun endElement(uri: String, localName: String, qName: String) {
        /* Flush old context into document (if required). */
        val parsers = this.mappings[this.xpath] ?: emptyList()
        for (parser in parsers) {
            parser.parse(this.buffer.toString(), this.document)
        }

        /* Pop stack. */
        this.stack.pop()
        this.xpath = "/${this.stack.joinToString("/")}"

        /* Reset string buffer. */
        this.buffer = StringBuffer()

        /* Flush old document (if needed). */
        if (this.xpath == this.newDocumentOn) {
            if (this.error) {
                LOGGER.warn("Skipping document due to parse error (uuid = ${this.document[Constants.FIELD_NAME_UUID]}).")
                this.error = false /* Clear error flag when new document starts. */
            } else {
                this.callback(this.document)
            }
            this.document = SolrInputDocument()
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
        LOGGER.error("SAX parse error encountered while parsing document (uuid = ${this.document[Constants.FIELD_NAME_UUID]}): ${e.message}.")
        this.error = true
    }
}