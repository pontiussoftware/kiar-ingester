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

        /* Map source collection. */
        element.appendChild(doc.createElement("dcterms:isPartOf").apply {
            val string = document.get<String>(Field.COLLECTION) ?: "Sammlung unbekannt"
            this.textContent = if (string.contains("Sammlung")) string else "Sammlung: $string"
        })

        /* Map source title. */
        element.appendChild(doc.createElement("dc:title").apply {
            this.textContent = document.get<String>(Field.DISPLAY)
        })

        /* Append publisher. */
        if (document.has(Field.PUBLISHER)) {
            document.getAll<String>(Field.PUBLISHER).forEach { publisher ->
                element.appendChild(doc.createElement("dc:publisher").apply {
                    this.textContent = publisher
                })
            }
        }

        /* Append publisher. */
        if (document.has(Field.OWNER)) {
            document.getAll<String>(Field.OWNER).forEach { owner ->
                element.appendChild(doc.createElement("dc:rights").apply {
                    this.textContent = owner
                })
            }
        }

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

        /* Append places. */
        listOf(Field.PLACE_CREATION, Field.PLACE_SHOWN, Field.PLACE_FINDING, Field.PLACE_PUBLICATION).forEach { field ->
            document.getAll<String>(field).forEach { subject ->
                element.appendChild(doc.createElement("dcterms:spatial").apply {
                    this.setAttribute("xml:lang", "de")
                    this.textContent = subject
                })
            }
        }

        /* Append dating information. */
        val dating = appendSpan(document, element)
        if (dating != null) {
            element.appendChild(doc.createElement("dcterms:created").apply {
                this.setAttribute("rdf:resource", dating)
            })
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
     * Extracts and appends dating information to the provided [Node].
     *
     * @param document The [SolrDocument] to extract information from.
     * @param appendTo The [Node] to append information to.
     * @return The 'rdf:about' identifier of the dating information.
     */
    private fun appendSpan(document: SolrDocument, appendTo: Node): String? {
        val datingDescription = document.get<String>(Field.DATING)
        val datingFrom = document.get<Double>(Field.DATING_FROM)
        val datingTo = document.get<Double>(Field.DATING_TO)
        if (datingDescription != null || datingFrom != null || datingTo != null) {
            val doc = appendTo.ownerDocument
            val identifier = "#kimnet:timespan:${document.get<String>(Field.UUID)}"
            appendTo.appendChild(doc.createElement("edm:TimeSpan").apply {
                this.appendChild(doc.createElement("skos:prefLabel").apply {
                    if (datingDescription != null) {
                        this.textContent = datingDescription
                    } else if (datingFrom != null && datingTo != null) {
                        this.textContent = "$datingFrom - $datingTo"
                    } else if (datingFrom != null) {
                        this.textContent = datingFrom.toString()
                    } else if (datingTo != null) {
                        this.textContent = datingTo.toString()
                    }
                    this.setAttribute("xml:lang", "de")
                })
                if (datingFrom != null) {
                    this.appendChild(doc.createElement("edm:begin").apply {
                        this.textContent = datingFrom.toString()
                    })
                }
                if (datingTo != null) {
                    this.appendChild(doc.createElement("edm:end").apply {
                        this.textContent = datingTo.toString()
                    })
                }
                this.setAttribute("rdf:about", identifier)
            })
            return identifier
        } else {
            return null
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
        rdfElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ore", "http://www.openarchives.org/ore/terms/")
        appendTo.appendChild(rdfElement)

        /* Create EDM element. */
        val edmElement = doc.createElement("edm:ProvidedCHO")
        rdfElement.appendChild(edmElement)

        /* Return. */
        return edmElement
    }
}