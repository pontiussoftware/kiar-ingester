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
        val identifier = "#kimnet:${document.get<String>(Field.UUID)}"
        val objectUrl = "https://www.kimnet.ch/objects/${document.get<String>(Field.UUID)}"
        element.setAttribute("rdf:about", identifier)

        /* Append ore:Aggregation element. */
        val oreAggregation = doc.createElement("ore:Aggregation")
        oreAggregation.setAttribute("rdf:about", identifier)
        oreAggregation.appendChild(doc.createElement("edm:aggregatedCHO").apply {
            this.setAttribute("rdf:resource", identifier)
        })

        oreAggregation.appendChild(doc.createElement("edm:isShownAt").apply {
            this.setAttribute("rdf:resource", objectUrl)
        })

        /* Set rights statement. */
        oreAggregation.appendChild(doc.createElement("edm:rights").apply {
            this.setAttribute("rdf:resource", rights)
        })

        element.appendChild(oreAggregation)

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

        /* Map source collection. */
        element.appendChild(doc.createElement("dcterms:isPartOf").apply {
            val string = document.get<String>(Field.COLLECTION) ?: "Sammlung unbekannt"
            this.textContent = if (string.contains("Sammlung")) string else "Sammlung: $string"
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

        /* Append publisher. */
        if (document.has(Field.PUBLISHER)) {
            document.getAll<String>(Field.PUBLISHER).forEach { publisher ->
                element.appendChild(doc.createElement("dc:publisher").apply {
                    this.textContent = publisher
                })
            }
        }

        /* Append creators. */
        listOf(Field.ARTIST, Field.PHOTOGRAPHER, Field.AUTHOR, Field.CREATOR).forEach { field ->
            document.getAll<String>(field).forEach { creator ->
                element.appendChild(doc.createElement("dc:creator").apply {
                    this.textContent = creator
                })
            }
        }

        /* Append material information */
        document.getAll<String>(Field.MATERIAL).forEach { material ->
            element.appendChild(doc.createElement("dcterms:medium").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = material
            })
        }

        /* Append technique information */
        document.getAll<String>(Field.TECHNIQUE).forEach { technique ->
            element.appendChild(doc.createElement("dc:type").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = technique
            })
        }

        /* Append subjects. */
        listOf(Field.ICONOGRAPHY, Field.SUBJECT, Field.TYPOLOGY).forEach { field ->
            document.getAll<String>(field).forEach { subject ->
                element.appendChild(doc.createElement("dc:subject").apply {
                    this.setAttribute("xml:lang", "de")
                    this.textContent = subject
                })
            }
        }

        /* Append images (& associated metadata). */
        var index = 0
        if (document.has(Field.PREVIEW)) {
            element.appendChild(doc.createElement("edm:type").apply {
                this.textContent = "IMAGE"
            })

            document.getAll<String>(Field.PREVIEW).forEach { url ->
                /* First image is appended to ore:Aggregation element as edm:isShownBy others as edm:hasView. */
                if (index == 0) {
                    oreAggregation.appendChild(doc.createElement("edm:isShownBy").apply {
                        this.setAttribute("rdf:resource", url)
                    })
                    oreAggregation.appendChild(doc.createElement("edm:object").apply {
                        this.setAttribute("rdf:resource", url)
                    })
                } else {
                    oreAggregation.appendChild(doc.createElement("edm:hasView").apply {
                        this.setAttribute("rdf:resource", url)
                    })
                }

                /* Every resource is appended to the edm:ProvidedCHO as edm:WebResource. */
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
        } else {
            element.appendChild(doc.createElement("edm:type").apply {
                this.textContent = "TEXT"
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