package ch.pontius.kiar.oai.mapper

import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.getAll
import ch.pontius.kiar.ingester.solrj.has
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object OAIDCMapper: OAIMapper {

    /** */
    private val languages = mapOf(
        "Deutsch" to "deu",
        "Französisch" to "fra",
        "Lateinisch" to "lat",
        "Italienisch" to "ita",
        "Englisch" to "eng",
        "Räteromanisch" to "fra",
        "Spanisch" to "spa"
    )

    /**
     * Maps the provided [SolrDocument] to an EDM element.
     *
     * @param appendTo The [Node] to which the EDM element should be appended.
     * @param document The [SolrDocument] to map.
     */
    override fun map(appendTo: Node, document: SolrDocument) {
        val element = this.emptyEdm(appendTo)
        val doc = appendTo.ownerDocument

        /* Map inventory number(s)* */
        element.appendChild(doc.createElement("dc:identifier").apply {
            this.textContent = document.get<String>(Field.INVENTORY_NUMBER)
        })

        /* Map source language. */
        element.appendChild(doc.createElement("dc:language").apply {
            val language = document.get<String>(Field.LANGUAGE) ?: "Deutsch"
            this.textContent = (languages[language] ?: language)
        })

        /* Map source institution. */
        element.appendChild(doc.createElement("dc:source").apply {
            this.textContent = document.get<String>(Field.INSTITUTION)
        })

        /* Map source title. */
        element.appendChild(doc.createElement("dc:title").apply {
            this.textContent = document.get<String>(Field.DISPLAY)
        })

        /* Map description. */
        if (document.has(Field.DESCRIPTION)) {
            element.appendChild(doc.createElement("dc:description").apply {
                this.textContent = document.get<String>(Field.DESCRIPTION)
            })
        }

        /* Add artist and creators. */
        document.getAll<String>(Field.ARTIST).forEach { artist ->
            element.appendChild(doc.createElement("dc:creator").apply {
                this.textContent = artist
            })
        }

        document.getAll<String>(Field.PHOTOGRAPHER).forEach { photographer ->
            element.appendChild(doc.createElement("dc:creator").apply {
                this.textContent = photographer
            })
        }

        document.getAll<String>(Field.AUTHOR).forEach { author ->
            element.appendChild(doc.createElement("dc:creator").apply {
                this.textContent = author
            })
        }

        document.getAll<String>(Field.CREATOR).forEach { creator ->
            element.appendChild(doc.createElement("dc:creator").apply {
                this.textContent = creator
            })
        }
    }

    /**
    * Creates and returns an empty EDM element.
    *
    * @param appendTo [Node] to which the EDM element should be appended.
    * @return [Element] representing the EDM element.
    */
    private fun emptyEdm(appendTo: Node): Element {
        val doc = appendTo.ownerDocument

        /* Create RDF element. */
        val dcElement = doc.createElement("oai_dc:dc")
        dcElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/")
        dcElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", "http://purl.org/dc/elements/1.1/")
        appendTo.appendChild(dcElement)

        /* Return. */
        return dcElement
    }
}