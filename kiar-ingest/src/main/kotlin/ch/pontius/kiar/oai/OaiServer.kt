package ch.pontius.kiar.oai

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.w3c.dom.Document
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
        const val PAGE_SIZE = 1000
    }

    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** A [ConcurrentHashMap] of [Http2SolrClient] used by this [OaiServer] to fetch data. */
    private val clients = ConcurrentHashMap<ApacheSolrConfig, Http2SolrClient>()

    /** A [ConcurrentHashMap] of resumption tokens and their state. */
    private val tokens = ConcurrentHashMap<String, Int>()

    /**
     *
     */
    fun handleIdentify(): Document {
        val doc = this.documentBuilder.newDocument()

        return doc
    }

    /**
     *
     */
    fun handleListIdentifiers(collection: String, resumptionToken: String? = null): Document {
        /* Fetch resumption token and start value. */
        val start = resumptionToken?.let { this.tokens[it] } ?: 0
        val token = resumptionToken ?: UUID.randomUUID().toString()
        val query = SolrQuery("*:*")
        query.addField("uuid")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val client = getOrLoadClient(collection)
        val response = client.query(collection, query)

        /* Construct response document. */
        val doc = this.documentBuilder.newDocument()

        /* Root element. */
        val rootElement = doc.createElement("OAI-PMH")
        doc.appendChild(rootElement)

        /* List record element. */
        val listRecordsElement = doc.createElement("ListIdentifiers")
        rootElement.appendChild(listRecordsElement)

        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("record")
            listRecordsElement.appendChild(recordElement)

            val headerElement = doc.createElement("header")
            recordElement.appendChild(headerElement)

            val identifierElement = doc.createElement("identifier")
            headerElement.appendChild(identifierElement)
            identifierElement.appendChild(doc.createTextNode(document.getFieldValue("uuid").toString()))
        }

        /* Store resumption token. */
        this.tokens[token] = start + PAGE_SIZE
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
     * Closes this OaiServer.
     */
    override fun close() {
        for (client in this.clients.values) {
            client.close()
        }
    }
}