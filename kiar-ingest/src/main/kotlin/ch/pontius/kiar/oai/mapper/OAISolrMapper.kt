package ch.pontius.kiar.oai.mapper

import ch.pontius.kiar.oai.Formats
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * [OAIMapper] implementation that maps to the OAI Apache Solr format.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object OAISolrMapper: OAIMapper {
    /** The [OAIDCMapper] returns the [Formats.SOLR] format. */
    override val format: Formats = Formats.SOLR

    /**
     * Maps the provided [SolrDocument] to an EDM element.
     *
     * @param appendTo The [Node] to which the EDM element should be appended.
     * @param document The [SolrDocument] to map.
     */
    override fun map(appendTo: Node, document: SolrDocument) {
        val doc = appendTo.ownerDocument ?: (appendTo as Document)

        /* Create root document element. */
        val docElement = doc.createElement("doc")
        appendTo.appendChild(docElement)

        /* Iterate through all fields in the SolrDocument, */
        document.fieldNames.forEach { fieldName ->
            val fieldValues = document.getFieldValues(fieldName)
            if (fieldValues != null) {
                if (fieldValues.size == 1) {
                    val fieldElement = fieldValues.first().toNode(doc) ?: return@forEach
                    fieldElement.setAttribute("name", fieldName)
                    docElement.appendChild(fieldElement)
                } else {
                    // Multi-value field
                    val arrayElement = doc.createElement("arr")
                    arrayElement.setAttribute("name", fieldName)
                    fieldValues.forEach { value ->
                        val fieldElement = value.toNode(doc) ?: return@forEach
                        arrayElement.appendChild(fieldElement)
                    }
                    docElement.appendChild(arrayElement)
                }
            }
        }
    }


    /**
     * Converts this [Any] value to an [Element] representation.
     *
     * @param doc The [Document] that should be used to create the [Element].
     * @return [Element] or null.
     */
    private fun Any.toNode(doc: Document): Element? = when(this) {
        is String -> doc.createElement("str").apply {
            this.textContent = this@toNode
        }
        is Boolean -> doc.createElement("bool").apply {
            this.textContent = this@toNode.toString()
        }
        is Int -> doc.createElement("int").apply {
            this.textContent = this@toNode.toString()
        }
        is Long -> doc.createElement("long").apply {
            this.textContent = this@toNode.toString()
        }
        is Float -> doc.createElement("float").apply {
            this.textContent = this@toNode.toString()
        }
        is Double -> doc.createElement("double").apply {
            this.textContent = this@toNode.toString()
        }
        else -> null
    }
}