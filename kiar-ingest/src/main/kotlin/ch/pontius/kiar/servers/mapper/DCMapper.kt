package ch.pontius.kiar.servers.mapper

import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.getAll
import ch.pontius.kiar.ingester.solrj.has
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * [Mapper] implementation that maps to the OAI DC format.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object DCMapper: Mapper {
    /** The [DCMapper] returns the [Formats.OAI_DC] format. */
    override val format: Formats = Formats.OAI_DC

    /**
     * Maps the provided [SolrDocument] to an EDM element.
     *
     * @param appendTo The [Node] to which the EDM element should be appended.
     * @param document The [SolrDocument] to map.
     */
    override fun map(appendTo: Node, document: SolrDocument) {
        val element = this.emptyElement(appendTo)
        val doc = appendTo.ownerDocument

        /* Map inventory number(s)* */
        element.appendChild(doc.createElement("dc:identifier").apply {
            this.textContent = document.get<String>(Field.INVENTORY_NUMBER)
        })

        /* Map source title. */
        element.appendChild(doc.createElement("dc:title").apply {
            this.textContent = document.get<String>(Field.DISPLAY)
        })

        /* Add artists and creators. */
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

        /* Map dating information. */
        if (document.has(Field.DATING)) {
            element.appendChild(doc.createElement("dc:date").apply {
                this.textContent = document.get<String>(Field.DATING)
            })
        }

        /* Map source institution. */
        element.appendChild(doc.createElement("dc:source").apply {
            this.textContent = document.get<String>(Field.INSTITUTION)
        })

        /* Map direct link */
        element.appendChild(doc.createElement("dc:identifier").apply {
            this.textContent = "https://www.kimnet.ch/objects/${document.get<String>(Field.UUID)}"
        })

        /* Add image links. */
        val previewUrl = document.getAll<String>(Field.PREVIEW).firstOrNull()
        if (previewUrl != null) {
            element.appendChild(doc.createElement("dc:identifier").apply {
                this.textContent = previewUrl
            })
        }

        /* Map description. */
        if (document.has(Field.DESCRIPTION)) {
            element.appendChild(doc.createElement("dc:description").apply {
                this.textContent = document.get<String>(Field.DESCRIPTION)
            })
        }
    }

    /**
    * Creates and returns an empty EDM element.
    *
    * @param appendTo [Node] to which the EDM element should be appended.
    * @return [Element] representing the EDM element.
    */
    private fun emptyElement(appendTo: Node): Element {
        val doc = appendTo.ownerDocument

        /* Create RDF element. */
        val dcElement = doc.createElement("dc")
        dcElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", "http://purl.org/dc/elements/1.1/")
        appendTo.appendChild(dcElement)

        /* Return. */
        return dcElement
    }
}