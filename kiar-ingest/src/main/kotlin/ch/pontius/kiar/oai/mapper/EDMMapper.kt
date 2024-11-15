package ch.pontius.kiar.oai.mapper

import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import ch.pontius.kiar.ingester.solrj.getAll
import ch.pontius.kiar.ingester.solrj.has
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.time.LocalDate

/**
 * [OAIMapper] implementation that maps to the Europeana Data Model (EDM).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class EDMMapper(store: TransientEntityStore): OAIMapper {

    companion object {

        /** Regular expression to match URL patterns. */
        private val URL_PATTERN_REGEX = "\\$\\{(\\w+)}".toRegex()

        /**
         * Replaces a URL pattern in the provided template with the actual values from the provided [SolrDocument].
         *
         * @param template The [String] template.
         * @param document The [SolrDocument] to extract values from.
         * @return Final URL
         */
        private fun replaceUrlPatterns(template: String, document: SolrDocument): String {
            return URL_PATTERN_REGEX.replace(template) { matchResult ->
                val fieldName = matchResult.groupValues[1]
                document.get(fieldName)?.toString() ?: throw IllegalStateException("Field '$fieldName' not found in document.")
            }
        }
    }


    /** A map of all institutions. */
    private val institutions: Map<String, Institution> = store.transactional(true) {
        DbInstitution.all().asSequence().map { it.name to it.toApi() }.toMap()
    }

    /**
     * Maps the provided [SolrDocument] to an EDM element.
     *
     * @param appendTo The [Node] to which the EDM element should be appended.
     * @param document The [SolrDocument] to map.
     */
    override fun map(appendTo: Node, document: SolrDocument) {
        /* Extract necessary information units. */
        val rights = document.get<String>(Field.RIGHTS_STATEMENT_URL) ?: return
        val institutionName = document.get<String>(Field.INSTITUTION) ?: return
        val institution: Institution = this.institutions[institutionName] ?: return
        val urlPattern = institution.defaultObjectUrl ?: "https://www.kimnet.ch/objects/\${uuid}"

        /* Extract necessary XML elements. */
        val rdfElement = this.emptyRdf(appendTo)
        val doc = appendTo.ownerDocument

        /* Prepare object identifier and object URL. */
        val identifier = "#kimnet:cho:${document.get<String>(Field.UUID)}"
        val objectUrl = try {
            replaceUrlPatterns(urlPattern, document)
        } catch (_: IllegalStateException) {
            return
        }

        /* Create and append edm:ProvidedCHO element. */
        val providedCHO = doc.createElement("edm:ProvidedCHO")
        rdfElement.appendChild(providedCHO)
        providedCHO.setAttribute("rdf:about", identifier)

        /* Map inventory number(s)* */
        providedCHO.appendChild(doc.createElement("dc:identifier").apply {
            this.textContent = document.get<String>(Field.INVENTORY_NUMBER)
        })

        /* Map object type */
        providedCHO.appendChild(doc.createElement("dc:subject").apply {
            this.textContent = document.get<String>(Field.OBJECTTYPE)
        })

        /* Map ISBN. */
        if (document.has(Field.ISBN)) {
            rdfElement.appendChild(doc.createElement("dc:identifier").apply {
                this.textContent = document.get<String>(Field.ISBN)
            })
        }

        /* Map source language. */
        providedCHO.appendChild(doc.createElement("dc:language").apply {
            val language = document.get<String>(Field.LANGUAGE) ?: "Deutsch"
            this.textContent = language
        })

        /* Map source institution. */
        providedCHO.appendChild(doc.createElement("dc:source").apply {
            this.textContent = document.get<String>(Field.INSTITUTION)
        })

        /* Map source collection. */
        val collection = document.get<String>(Field.COLLECTION)
        if (collection != null) {
            providedCHO.appendChild(doc.createElement("dcterms:isPartOf").apply {
                this.textContent = if (collection.contains("sammlung", ignoreCase = true)) collection else "Sammlung: $collection"
            })
        }

        /* Map source title. */
        providedCHO.appendChild(doc.createElement("dc:title").apply {
            this.setAttribute("xml:lang", "de")
            this.textContent = document.get<String>(Field.DISPLAY)
        })

        /* Append publisher. */
        if (document.has(Field.PUBLISHER)) {
            document.getAll<String>(Field.PUBLISHER).forEach { publisher ->
                providedCHO.appendChild(doc.createElement("dc:publisher").apply {
                    this.textContent = publisher
                })
            }
        }

        /* Append owner. */
        if (document.has(Field.OWNER)) {
            document.getAll<String>(Field.OWNER).forEach { owner ->
                providedCHO.appendChild(doc.createElement("dc:rights").apply {
                    this.textContent = owner
                })
            }
        }

        /* Map description. */
        if (document.has(Field.DESCRIPTION)) {
            providedCHO.appendChild(doc.createElement("dc:description").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = document.get<String>(Field.DESCRIPTION)
            })
        }

        /* Map alternative designation. */
        if (document.has(Field.ALTERNATIVE_DESIGNATION)) {
            providedCHO.appendChild(doc.createElement("dcterms:alternative").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = document.get<String>(Field.ALTERNATIVE_DESIGNATION)
            })
        }

        /* Append creators. */
        listOf(Field.ARTIST, Field.PHOTOGRAPHER, Field.AUTHOR, Field.CREATOR).forEach { field ->
            document.getAll<String>(field).forEach { creator ->
                providedCHO.appendChild(doc.createElement("dc:creator").apply {
                    this.textContent = creator
                })
            }
        }

        /* Append material information */
        document.getAll<String>(Field.MATERIAL).forEach { material ->
            providedCHO.appendChild(doc.createElement("dcterms:medium").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = material
            })
        }

        /* Append technique information */
        document.getAll<String>(Field.TECHNIQUE).forEach { technique ->
            providedCHO.appendChild(doc.createElement("dc:type").apply {
                this.setAttribute("xml:lang", "de")
                this.textContent = technique
            })
        }

        /* Append subjects. */
        listOf(Field.ICONOGRAPHY, Field.SUBJECT, Field.TYPOLOGY).forEach { field ->
            document.getAll<String>(field).forEach { subject ->
                providedCHO.appendChild(doc.createElement("dc:subject").apply {
                    this.setAttribute("xml:lang", "de")
                    this.textContent = subject
                })
            }
        }

        /* Append places. */
        listOf(Field.PLACE_CREATION, Field.PLACE_SHOWN, Field.PLACE_PUBLICATION).forEach { field ->
            document.getAll<String>(field).forEach { subject ->
                providedCHO.appendChild(doc.createElement("dcterms:spatial").apply {
                    this.setAttribute("xml:lang", "de")
                    this.textContent = subject
                })
            }
        }

        /* Append finding location (if available). */
        val findingLocation = appendFindingLocation(document, rdfElement)
        if (findingLocation != null) {
            providedCHO.appendChild(doc.createElement("dcterms:spatial").apply {
                this.setAttribute("rdf:resource", findingLocation)
            })
        }

        /* Append current location (if available). */
        val currentLocation = appendCurrentLocation(institution, document, rdfElement)
        if (currentLocation != null) {
            providedCHO.appendChild(doc.createElement("edm:currentLocation").apply {
                this.setAttribute("rdf:resource", currentLocation)
            })
        }

        /* Append dating information. */
        val dating = appendCreationDate(document, rdfElement)
        if (dating != null) {
            providedCHO.appendChild(doc.createElement("dcterms:created").apply {
                this.setAttribute("rdf:resource", dating)
            })
        }

        /* Append images (& associated metadata) as edm:WebResource. */
        var index = 0
        val previews = document.getAll<String>(Field.PREVIEW)
        if (previews.isNotEmpty()) {
            providedCHO.appendChild(doc.createElement("edm:type").apply {
                this.textContent = "IMAGE"
            })

            document.getAll<String>(Field.PREVIEW).forEach { url ->
                /* Every resource is appended to the edm:ProvidedCHO as edm:WebResource. */
                val webResourceElement = doc.createElement("edm:WebResource")
                webResourceElement.setAttribute("rdf:about", url)
                rdfElement.appendChild(webResourceElement)

                /* Set creator and rights information. */
                val artist = document.getAll<String>(Field.IMAGE_ARTISTS).getOrNull(index)
                if (!artist.isNullOrBlank()) {
                    webResourceElement.appendChild(doc.createElement("dc:creator").apply {
                        this.textContent = artist
                    })
                }

                /* We always export jpegs. */
                webResourceElement.appendChild(doc.createElement("dc:format").apply {
                    this.textContent = "image/jpeg"
                })

                val copyright = document.getAll<String>(Field.IMAGE_COPYRIGHT).getOrNull(index)
                if (!copyright.isNullOrBlank()) {
                    webResourceElement.appendChild(doc.createElement("dc:rights").apply {
                        this.textContent = copyright
                    })
                }

                /* We always export jpegs. */
                if (index > 0) {
                    webResourceElement.appendChild(doc.createElement("edm:isNextInSequence").apply {
                        this.setAttribute("rdf:resource", previews[index - 1])
                    })
                }
                index++
            }
        } else {
            providedCHO.appendChild(doc.createElement("edm:type").apply {
                this.textContent = "TEXT"
            })
        }

        /* Append ore:Aggregation element (IMPORTANT: Order of elements is relevant). */
        val oreAggregation = doc.createElement("ore:Aggregation")
        rdfElement.appendChild(oreAggregation)
        oreAggregation.setAttribute("rdf:about", identifier)
        oreAggregation.appendChild(doc.createElement("edm:aggregatedCHO").apply {
            this.setAttribute("rdf:resource", identifier)
        })

        /* Set data provider. */
        oreAggregation.appendChild(doc.createElement("edm:dataProvider").apply {
            this.textContent = document.get<String>(Field.INSTITUTION)
        })

        /* Append edm:hasView elements for every preview except the first. */
        if (previews.size > 1) {
            for (i in 1 until previews.size) {
                oreAggregation.appendChild(doc.createElement("edm:hasView").apply {
                    this.setAttribute("rdf:resource", previews[i])
                })
            }
        }

        /* Append edm:isShownAt elements. */
        oreAggregation.appendChild(doc.createElement("edm:isShownAt").apply {
            this.setAttribute("rdf:resource", objectUrl)
        })

        /* Append edm:isShownBy and edm:object element for first preview. */
        if (previews.isNotEmpty()) {
            oreAggregation.appendChild(doc.createElement("edm:isShownBy").apply {
                this.setAttribute("rdf:resource", previews.first())
            })

            oreAggregation.appendChild(doc.createElement("edm:object").apply {
                this.setAttribute("rdf:resource", previews.first())
            })
        }

        /* Set data provider. */
        oreAggregation.appendChild(doc.createElement("edm:provider").apply {
            this.textContent = "CARARE"
        })

        /* Set rights statement URL. */
        oreAggregation.appendChild(doc.createElement("edm:rights").apply {
            this.setAttribute("rdf:resource", rights.replace("https://", "http://"))
        })

        /* Set intermediate data provider. */
        oreAggregation.appendChild(doc.createElement("edm:intermediateProvider").apply {
            this.textContent = "Kulturerbe Informationsmanagement Schweiz (KIMnet)"
        })
    }

    /**
     * Extracts and appends location information of the holding [Institution] to the provided [Node].
     *
     * @param institution The [Institution] to extract information from.
     * @param document The [SolrDocument] to extract information from.
     * @param appendTo The [Node] to append information to.
     * @return The 'rdf:about' identifier of the dating information.
     */
    private fun appendCurrentLocation(institution: Institution, document: SolrDocument, appendTo: Node): String? {
        if (institution.longitude != null && institution.latitude != null) {
            val institutionIdentifier = "#kimnet:current-loc:${document.get<String>(Field.UUID)}"
            val doc = appendTo.ownerDocument
            appendTo.appendChild(doc.createElement("edm:Place").apply {
                this.setAttribute("rdf:about", institutionIdentifier)
                this.appendChild(doc.createElement("wgs84_pos:lat").apply {
                    this.textContent = institution.latitude.toString()
                })
                this.appendChild(doc.createElement("wgs84_pos:long").apply {
                    this.textContent = institution.longitude.toString()
                })
                this.appendChild(doc.createElement("skos:prefLabel").apply {
                    this.setAttribute("xml:lang", "de")
                    this.textContent = institution.city
                })
            })
            return institutionIdentifier
        } else {
            return null
        }
    }

    /**
     * Extracts and appends dating information to the provided [Node].
     *
     * @param document The [SolrDocument] to extract information from.
     * @param appendTo The [Node] to append information to.
     * @return The 'rdf:about' identifier of the dating information.
     */
    private fun appendFindingLocation(document: SolrDocument, appendTo: Node): String? {
        if (document.has(Field.PLACE_FINDING)) {
            val findingLocation = document.getAll<String>(Field.PLACE_FINDING).first()
            val doc = appendTo.ownerDocument
            val identifier = "#kimnet:finding-location:${document.get<String>(Field.UUID)}"
            appendTo.appendChild(doc.createElement("edm:Place").apply {
                this.setAttribute("rdf:about", identifier)
                val coordinates = document.get<String>(Field.COORDINATES)?.split(",")?.mapNotNull { it.toDoubleOrNull() }
                if (coordinates?.size == 2) {
                    this.appendChild(doc.createElement("wgs84_pos:lat").apply {
                        this.textContent = coordinates[0].toString()
                    })
                    this.appendChild(doc.createElement("wgs84_pos:long").apply {
                        this.textContent = coordinates[1].toString()
                    })
                    this.appendChild(doc.createElement("skos:prefLabel").apply {
                        this.setAttribute("xml:lang", "de")
                        this.textContent = findingLocation
                    })
                }
            })
            return identifier
        } else {
            return null
        }
    }

    /**
     * Extracts and appends dating information to the provided [Node].
     *
     * @param document The [SolrDocument] to extract information from.
     * @param appendTo The [Node] to append information to.
     * @return The 'rdf:about' identifier of the dating information.
     */
    private fun appendCreationDate(document: SolrDocument, appendTo: Node): String? {
        val datingDescription = document.get<String>(Field.DATING)
        val datingFrom = document.get<Float>(Field.DATING_FROM)
        val datingTo = document.get<Float>(Field.DATING_TO)
        if (datingDescription != null || datingFrom != null || datingTo != null) {
            val doc = appendTo.ownerDocument
            val identifier = "#kimnet:created:${document.get<String>(Field.UUID)}"
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
                        this.textContent = decimalToEDTF(datingFrom).toString()
                    })
                }
                if (datingTo != null) {
                    this.appendChild(doc.createElement("edm:end").apply {
                        this.textContent = decimalToEDTF(datingTo).toString()
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
    private fun emptyRdf(appendTo: Node): Element {
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

        /* Return. */
        return rdfElement
    }

    /**
     * Converts a decimal date to a [LocalDate].
     *
     * @param decimalDate The decimal date to convert.
     * @return [LocalDate]
     */
    private fun decimalToEDTF(decimalDate: Float): String {
        val year = decimalDate.toInt()
        var fractionalPart = decimalDate - year

        /* Extract month. */
        var month = 0
        if (fractionalPart > 0f) {
            month = (fractionalPart * 100f).toInt()
            fractionalPart = fractionalPart - (month / 100f)
        }

        /* Extract day. */
        var day = 0
        if (fractionalPart > 0f) {
            day = (fractionalPart * 10000f).toInt()
        }

        return when {
            day > 0 -> String.format("%04d-%02d-%02d", year, month, day)
            month > 0 -> String.format("%04d-%02d", year, month)
            else -> year.toString()
        }
    }
}