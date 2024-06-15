package ch.pontius.kiar.oai.mapper

import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.getAll
import ch.pontius.kiar.ingester.solrj.has
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * [OAIMapper] implementation that maps to the Europeana Data Model (EDM).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object EDMMapper: OAIMapper {
    /**
     * Maps the provided [SolrDocument] to an EDM element.
     *
     * @param appendTo The [Node] to which the EDM element should be appended.
     * @param document The [SolrDocument] to map.
     */
    override fun map(appendTo: Node, document: SolrDocument) {

        val rights = document.get<String>(Field.RIGHTS_STATEMENT_URL) ?: return

        val element = this.emptyEdm(appendTo)
        val doc = element.ownerDocument

        /* Set RDF about attribute. */
        element.setAttribute("rdf:about", "https://www.kimnet.ch/objects/${document.get<String>(Field.UUID)}")

        /* Rights statement URL. */
        element.appendChild(doc.createElement("edm:right").apply {
            this.setAttribute("rdf:resource", rights)
        })

        /* Map inventory number(s)* */
        element.appendChild(doc.createElement("dc:identifier").apply {
            this.textContent = document.get<String>(Field.INVENTORY_NUMBER)
        })

        /* Map ISBN. */
        if (document.has(Field.ISBN)) {
            element.appendChild(doc.createElement("dc:identifier").apply {
                this.textContent = document.get<String>(Field.ISBN)
            })
        }

        /* Map source language. */
        element.appendChild(doc.createElement("dc:language").apply {
            val language = document.get<String>(Field.LANGUAGE) ?: "Deutsch"
            this.textContent = language
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

        /* Map alternative designation. */
        if (document.has(Field.ALTERNATIVE_DESIGNATION)) {
            element.appendChild(doc.createElement("dcterms:alternative").apply {
                this.textContent = document.get<String>(Field.ALTERNATIVE_DESIGNATION)
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


        /* Append images (& associated metadata). */
        var index = 0
        if (document.has(Field.PREVIEW)) {
            document.getAll<String>(Field.PREVIEW).forEach { url ->
                val resource = doc.createElement("edm:WebResource")
                resource.appendChild(doc.createElement("rdf:about").apply {
                    this.textContent = url
                })

                val artist = document.getAll<String>(Field.IMAGE_ARTISTS).getOrNull(index)
                if (!artist.isNullOrBlank()) {
                    resource.appendChild(doc.createElement("dc:creator").apply {
                        this.textContent = artist
                    })
                }

                val copyright = document.getAll<String>(Field.IMAGE_COPYRIGHT).getOrNull(index)
                if (!copyright.isNullOrBlank()) {
                    resource.appendChild(doc.createElement("dc:rights").apply {
                        this.textContent = copyright
                    })
                }

                element.appendChild(resource)
                index++
            }
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
        val rdfElement = doc.createElement("rdf:RDF")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:edm", "http://www.europeana.eu/schemas/edm/")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:wgs84_pos", "http://www.w3.org/2003/01/geo/wgs84_pos#")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:foaf", "http://xmlns.com/foaf/0.1/")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rdaGr2", "http://rdvocab.info/ElementsGr2/")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:owl", "http://www.w3.org/2002/07/owl#")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:skos", "http://www.w3.org/2004/02/skos/core#")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dcterms", "http://purl.org/dc/terms/")
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", "http://purl.org/dc/elements/1.1/")
        appendTo.appendChild(rdfElement)

        /* Create EDM element. */
        val edmElement = doc.createElement("edm:ProvidedCHO")
        rdfElement.appendChild(edmElement)

        /* Return. */
        return edmElement
    }
}