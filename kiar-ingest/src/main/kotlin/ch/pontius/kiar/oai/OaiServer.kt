package ch.pontius.kiar.oai

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.ingester.solrj.uuid
import ch.pontius.kiar.oai.mapper.EDMMapper
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.Closeable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class OaiServer(private val store: TransientEntityStore): Closeable {

    companion object {
        const val PAGE_SIZE = 100
    }

    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** A [ConcurrentHashMap] of [Http2SolrClient] used by this [OaiServer] to fetch data. */
    private val clients = ConcurrentHashMap<ApacheSolrConfig, Http2SolrClient>()

    /** A [ConcurrentHashMap] of [Http2SolrClient] used by this [OaiServer] to fetch data. */
    private val tokens = ConcurrentHashMap<String, Int>()

    /**
     *
     */
    fun handleIdentify(): Document {
        val doc = this.documentBuilder.newDocument()

        return doc
    }

    /**
     * Handles the OAI-PMH verb "ListIdentifiers".
     *
     * @param collection The [String] name of the collection to harvest.
     * @param resumptionToken The optional resumption token.
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListRecords(collection: String, resumptionToken: String? = null): Document {
        /* Parse resumption token and start value. */
        val start = resumptionToken?.let { this.tokens[it] ?: throw IllegalArgumentException("Invalid resumption token.") } ?: 0
        val verb = "ListIdentifiers"

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val client = getOrLoadClient(collection)
        val response = client.query(collection, query)

        /* Construct response document. */
        val (doc, root) = this.documentBuilder.generateResponse(verb)

        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("oai:record")
            root.appendChild(recordElement)

            val headerElement = doc.createElement("oai:header")
            recordElement.appendChild(headerElement)

            val identifierElement = doc.createElement("oai:identifier")
            headerElement.appendChild(identifierElement)
            identifierElement.appendChild(doc.createTextNode(document.uuid()))

            /* Map and append metadata. */
            val metadataElement = doc.createElement("oai:metadata")
            EDMMapper.map(metadataElement, document)
            recordElement.appendChild(metadataElement)
        }

        /* If there are more documents to return, include a resumptionToken. */
        val lastElement = start + PAGE_SIZE
        if (response.results.numFound > lastElement) {
            /* Update resumption token. */
            val newToken = UUID.randomUUID().toString()
            this.tokens[newToken] = lastElement

            /* Include new token in response. */
            val resumptionTokenElement = doc.createElement("oai:resumptionToken")
            resumptionTokenElement.setAttribute("completeListSize", "${response.results.numFound}")
            resumptionTokenElement.setAttribute("cursor", "$lastElement")
            resumptionTokenElement.appendChild(doc.createTextNode(newToken))
            root.appendChild(resumptionTokenElement)
        }

        /* Store resumption token. */
        return doc
    }

    /**
     * Handles the OAI-PMH verb "ListIdentifiers".
     *
     * @param collection The [String] name of the collection to harvest.
     * @param resumptionToken The optional resumption token.
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListIdentifiers(collection: String, resumptionToken: String? = null): Document {
        /* Parse resumption token and start value. */
        val start = resumptionToken?.let { this.tokens[it] ?: throw IllegalArgumentException("Invalid resumption token.") } ?: 0
        val verb = "ListIdentifiers"

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        query.addField("uuid")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val client = getOrLoadClient(collection)
        val response = client.query(collection, query)

        /* Construct response document. */
        val (doc, root) = this.documentBuilder.generateResponse(verb)

        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("oai:record")
            root.appendChild(recordElement)

            val headerElement = doc.createElement("oai:header")
            recordElement.appendChild(headerElement)

            val identifierElement = doc.createElement("oai:identifier")
            headerElement.appendChild(identifierElement)
            identifierElement.appendChild(doc.createTextNode(document.uuid()))
        }

        /* If there are more documents to return, include a resumptionToken. */
        val lastElement = start + PAGE_SIZE
        if (response.results.numFound > lastElement) {
            /* Update resumption token. */
            val newToken = UUID.randomUUID().toString()
            this.tokens[newToken] = lastElement

            /* Include new token in response. */
            val resumptionTokenElement = doc.createElement("oai:resumptionToken")
            resumptionTokenElement.setAttribute("oai:completeListSize", "${response.results.numFound}")
            resumptionTokenElement.setAttribute("cursor", "$lastElement")
            resumptionTokenElement.appendChild(doc.createTextNode(newToken))
            root.appendChild(resumptionTokenElement)
        }

        /* Store resumption token. */
        return doc
    }

    /**
     * Loads the configuration for the Apache Solr client.
     */
    private fun getOrLoadClient(collection: String): Http2SolrClient {
        val config = this.store.transactional(true) {
            DbCollection.filter {  (it.name eq collection) and (it.type.name eq DbCollectionType.OBJECT.name) }.firstOrNull()?.solr?.toApi() ?: throw IllegalArgumentException("Collection '$collection' does not exist.")
        }

        return this.clients.computeIfAbsent(config) {
            /* Prepare builder */
            var httpBuilder = Http2SolrClient.Builder(config.server)
            if (config.username != null && config.password != null) {
                httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
            }
            /* Prepare Apache Solr client. */
            httpBuilder.build()
        }
    }


    /**
     * Generates an empty OAI-PMH response document.
     *
     * @param verb The OAI-PMH verb.
     * @return [Pair] of [Document] and root [Element]
     */
    private fun DocumentBuilder.generateResponse(verb: String): Pair<Document, Element> {
        /* Construct response document. */
        val doc = this.newDocument()

        /* Root element. */
        val rootElement = doc.createElement("OAI-PMH")
        rootElement.setAttributeNodeNS(doc.createAttributeNS("http://www.openarchives.org/OAI/2.0/", "oai"))
        rootElement.setAttributeNodeNS(doc.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance/", "xsi"))
        rootElement.setAttributeNodeNS(doc.createAttribute("xsi:schemaLocation")?.also { it.value = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd" })
        doc.appendChild(rootElement)

        /* Append response date. */
        val responseDate = doc.createElement("oai:responseDate")
        responseDate.textContent = Date().toString()
        rootElement.appendChild(responseDate)

        /* Append response date. */
        val request = doc.createElement("oai:request")
        request.setAttribute("verb", verb)
        rootElement.appendChild(request)

        /* Verb element. */
        val verbElement = doc.createElement(verb)
        rootElement.appendChild(verbElement)

        return doc to rootElement
    }

    /**
     * Closes this OaiServer.
     */
    override fun close() {
        for (client in this.clients.values) {
            client.close()
        }
    }
}