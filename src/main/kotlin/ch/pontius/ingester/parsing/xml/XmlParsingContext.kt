package ch.pontius.ingester.parsing.xml

import ch.pontius.ingester.config.MappingConfig
import ch.pontius.ingester.parsing.values.ValueParser
import ch.pontius.ingester.solrj.Constants
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
 * @version 1.0.0
 */
class XmlParsingContext(config: MappingConfig, val callback: (SolrInputDocument) -> Unit): DefaultHandler() {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The current [SolrInputDocument] that is being processed. */
    private val document = SolrInputDocument()

    /** The current XPath this [XmlParsingContext] is currently in. */
    private var xpath = "/"

    /** The current XPath this [XmlParsingContext] is currently in. */
    private val stack = Stack<String>()

    /** The current XPath this [XmlParsingContext] is currently in. */
    private val parsers = HashMap<XmlAttributeMapping, ValueParser<*>>()

    /** The longest, common prefix found for all [XmlAttributeMapping]. This prefix will be used to distinguish between different objects. */
    private val mappings = HashMap<String, MutableList<XmlAttributeMapping>>()

    /** The longest, common prefix found for all [XmlAttributeMapping]. This prefix will be used to distinguish between different objects. */
    private val newDocumentOn: String

    /** An internal error flag */
    private var error: Boolean = false

    init {
        var commonPrefix = config.values.first().source
        config.values.forEach {
            this.mappings.compute(it.source) { k, v ->
                var list = v
                if (list == null) {
                    list = mutableListOf()
                }
                list.add(it)
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

        /* Create parsers for mappings. */
        val mappings = this.mappings[this.xpath]
        if (mappings != null) {
            for (m in mappings) {
                this.parsers[m] = m.parser.newInstance(m.parameters)
            }
        }
    }

    /**
     * Removes the new element from the stack
     */
    override fun endElement(uri: String, localName: String, qName: String) {
        /* Flush old context into document (if required). */
        val previousMappings = this.mappings[this.xpath]
        if (previousMappings != null) {
            for (m in previousMappings) {
                val value = this.parsers[m]?.get()
                if (value != null) {
                    if (m.multiValued) {
                        this.document.addField(m.destination, value)
                    } else {
                        this.document.setField(m.destination, value)
                    }
                }
            }
        }
        this.parsers.clear()

        /* Pop stack. */
        this.stack.pop()
        this.xpath = "/${this.stack.joinToString("/")}"

        /* Flush old document (if needed). */
        if (this.xpath == this.newDocumentOn) {
            if (this.error) {
                LOGGER.warn("Skipping document due to parse error (uuid = ${this.document[Constants.FIELD_NAME_UUID]}).")
                this.error = false /* Clear error flag when new document starts. */
            } else {
                this.callback(this.document.deepCopy())
            }
            this.document.clear()
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
        val mappings = this.mappings[this.xpath]
        if (mappings != null) {
            for (m in mappings) {
                this.parsers[m]?.parse(String(ch.copyOfRange(start, start + length)))
            }
        }
    }

    /**
     * Handles [SAXParseException].
     *
     * @param e [SAXParseException]
     */
    override fun error(e: SAXParseException) {
        LOGGER.error("SAX parse error encountered while parsing document: ${e.message}.")
        this.error = true
    }
}