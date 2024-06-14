package ch.pontius.kiar.oai

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.ingester.solrj.uuid
import ch.pontius.kiar.oai.mapper.EDMMapper
import io.javalin.http.Context
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.Closeable
import java.text.DateFormat
import java.text.SimpleDateFormat
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

    /** The [DateFormat] used by this [OaiServer]. */
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    init {
        df.timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Handles a error during OAI-PMH processing.
     *
     * @param code The error code.
     * @param message The error message.
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleError(code: String, message: String): Document  {
        val root = this.documentBuilder.generateResponse("error")
        root.textContent = message
        root.setAttribute("code", code)
        return root.ownerDocument
    }

    /**
     * Handles the OAI-PMH verb "Identify".
     *
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleIdentify(ctx: Context): Document {
        val collection = ctx.pathParam("collection")
        val root = this.documentBuilder.generateResponse("Identify")
        root.appendChild(root.ownerDocument.createElement("repositoryName").apply { textContent = "Kiar" })
        root.appendChild(root.ownerDocument.createElement("baseURL").apply { textContent = "https://ingest.kimnet.ch/api/$collection/oai-pmh" })
        root.appendChild(root.ownerDocument.createElement("protocolVersion").apply { textContent = "2.0" })
        root.appendChild(root.ownerDocument.createElement("adminEmail").apply { textContent = "info@kimnet.ch" })
        root.appendChild(root.ownerDocument.createElement("earliestDatestamp").apply { textContent = "2024-01-01" })
        root.appendChild(root.ownerDocument.createElement("deletedRecord").apply { textContent = "no" })
        root.appendChild(root.ownerDocument.createElement("granularity").apply { textContent = "YYYY-MM-DD" })
        return root.ownerDocument
    }

    /**
     * Handles the OAI-PMH verb "ListSets".
     *
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListSets(): Document {
        val verb = "ListSets"

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse(verb)
        root.appendChild(root.ownerDocument.createElement("error").apply {
            this.setAttribute("code", "noSetHierarchy")
            this.textContent = "This repository does not support sets."
        })

        return root.ownerDocument
    }

    /**
     * Handles the OAI-PMH verb "ListSets".
     *
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListMetadataFormats(): Document {
        val verb = "ListMetadataFormats"

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse(verb)
        Formats.entries.forEach { format ->
            val formatElement = root.ownerDocument.createElement("metadataFormat")
            formatElement.appendChild(root.ownerDocument.createElement("metadataPrefix").apply { textContent = format.prefix })
            formatElement.appendChild(root.ownerDocument.createElement("schema").apply { textContent = format.schema })
            formatElement.appendChild(root.ownerDocument.createElement("metadataNamespace").apply { textContent = format.namespace })
            root.appendChild(formatElement)
        }

        return root.ownerDocument
    }

    /**
     * Handles the OAI-PMH verb "ListIdentifiers".
     *
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleGetRecord(ctx: Context): Document {
        val collection = ctx.pathParam("collection")
        val identifier = ctx.queryParam("identifier") ?: return handleError("badArgument", "Missing identifier.")

        /* Obtain client and query for entry. */
        val client = getOrLoadClient(collection)
        val response = client.getById(collection, identifier)

        /* Generate response document. */
        val root = this.documentBuilder.generateResponse("GetRecord")
        val doc = root.ownerDocument

        /* Handle error case. */
        if (response == null) {
            root.appendChild(doc.createElement("error").apply {
                this.setAttribute("code", "idDoesNotExist")
                this.textContent = "The provided identifier does not exist."
            })
            return doc
        }

        val recordElement = doc.createElement("record")
        root.appendChild(recordElement)

        /* Create header element. */
        val headerElement = doc.createElement("header")
        headerElement.appendChild(doc.createElement("identifier").apply { textContent = response.uuid()})
        headerElement.appendChild(doc.createElement("datestamp").apply { textContent = "2024-01-01" })
        recordElement.appendChild(headerElement)

        /* Map and append metadata. */
        val metadataElement = doc.createElement("metadata")
        EDMMapper.map(metadataElement, response)
        recordElement.appendChild(metadataElement)

        return doc
    }

    /**
     * Handles the OAI-PMH verb "ListRecords".
     *
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListRecords(ctx: Context): Document {
        val collection = ctx.pathParam("collection")
        val token = ctx.queryParam("resumptionToken")

        /* Determine start based on resumption token. */
        val start = if (token != null) {
            this.tokens[token] ?: return handleError("badResumptionToken", "Invalid resumption token.")
        } else {
            0
        }

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val client = getOrLoadClient(collection)
        val response = client.query(collection, query)

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse("ListRecords")
        val doc = root.ownerDocument


        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("record")
            root.appendChild(recordElement)

            val headerElement = doc.createElement("header")
            recordElement.appendChild(headerElement)

            /* Create header element. */
            headerElement.appendChild(doc.createElement("identifier").apply { textContent = document.uuid()})
            headerElement.appendChild(doc.createElement("datestamp").apply { textContent = "2024-01-01" })
            recordElement.appendChild(headerElement)

            /* Map and append metadata. */
            val metadataElement = doc.createElement("metadata")
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
            val resumptionTokenElement = doc.createElement("resumptionToken")
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
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the OAI-PMH response.
     */
    fun handleListIdentifiers(ctx: Context): Document {
        val collection = ctx.pathParam("collection")
        val token = ctx.queryParam("resumptionToken")

        /* Determine start based on resumption token. */
        val start = if (token != null) {
            this.tokens[token] ?: return handleError("badResumptionToken", "Invalid resumption token.")
        } else {
            0
        }

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        query.addField("uuid")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val client = getOrLoadClient(collection)
        val response = client.query(collection, query)
        if (response.results.numFound == 0L) {
            return handleError("noRecordsMatch", "No records match the query.")
        }

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse("ListIdentifiers")
        val doc = root.ownerDocument

        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("record")
            root.appendChild(recordElement)

            val headerElement = doc.createElement("header")
            recordElement.appendChild(headerElement)

            val identifierElement = doc.createElement("identifier")
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
            val resumptionTokenElement = doc.createElement("resumptionToken")
            resumptionTokenElement.setAttribute("completeListSize", "${response.results.numFound}")
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
            DbCollection.filter { (it.name eq collection) and (it.type.name eq DbCollectionType.OBJECT.name) and (it.oai eq true) }.firstOrNull()?.solr?.toApi()
                ?: throw IllegalArgumentException("Collection '$collection' does not exist.")
        }

        return this.clients.computeIfAbsent(config) {
            /* Prepare builder */
            var httpBuilder = Http2SolrClient.Builder(config.server)
            httpBuilder.useHttp1_1(true)
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
    private fun DocumentBuilder.generateResponse(verb: String):Element {
        /* Construct response document. */
        val doc = this.newDocument()

        /* Root element. */
        val rootElement = doc.createElement("OAI-PMH")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.openarchives.org/OAI/2.0/")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        rootElement.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd")
        doc.appendChild(rootElement)

        /* Append response date. */
        val responseDate = doc.createElement("responseDate")
        responseDate.textContent = this@OaiServer.df.format(Date())
        rootElement.appendChild(responseDate)

        /* Append response date. */
        val request = doc.createElement("request")
        request.setAttribute("verb", verb)
        rootElement.appendChild(request)

        /* Verb element. */
        val verbElement = doc.createElement(verb)
        rootElement.appendChild(verbElement)

        return verbElement
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