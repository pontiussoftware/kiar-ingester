package ch.pontius.kiar.oai.mapper

import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.get
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
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
        val element = this.emptyEdm(appendTo)
        val doc = element.ownerDocument

        /* Set RDF about attribute. */
        element.setAttribute("rdf:about", "https://www.kimnet.ch/objects/${document.get<String>(Field.UUID)}")

        /* Map inventory number(s)* */
        doc.createElement("dc:identifier").apply {
            this.textContent = document.get<String>(Field.INVENTORY_NUMBER)
            element.appendChild(this)
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
        appendTo.appendChild(rdfElement)

        /* Create EDM element. */
        val edmElement = doc.createElement("edm:ProvidedCHO")
        rdfElement.appendChild(edmElement)

        /* Return. */
        return edmElement
    }
}