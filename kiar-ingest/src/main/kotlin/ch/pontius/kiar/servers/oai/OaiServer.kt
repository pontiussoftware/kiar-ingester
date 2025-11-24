package ch.pontius.kiar.servers.oai

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.uuid
import ch.pontius.kiar.servers.mapper.Formats
import ch.pontius.kiar.servers.mapper.Mapper
import ch.pontius.kiar.servers.oai.Verbs.*
import ch.pontius.kiar.solr.SolrClientProvider
import com.github.benmanes.caffeine.cache.Caffeine
import io.javalin.http.Context
import io.javalin.http.HandlerType
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


/**
 * A simple OAI-PMH server implementation for harvesting data contained in collections.
 *
 * See [OAI-PMH Protocol](https://www.openarchives.org/OAI/openarchivesprotocol.html) for more information.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class OaiServer() {
    companion object {
        /** The page size for the OAI endpoint. */
        const val PAGE_SIZE = 100

        /** The simple date format used for queries. */
        private val GRANULARITY_FORMAT = SimpleDateFormat("yyyy-MM-dd")
    }

    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** A [ConcurrentHashMap] of [Http2SolrClient] used by this [OaiServer] to fetch data. */
    private val tokens = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(60)).build<String, Triple<Int, String?, Mapper>>().asMap()

    /** A cache of [Http2SolrClient]s used by this data ingest server. */
    private val collections = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).build<String, ApacheSolrConfig?> { collection ->
        transaction {
            (SolrCollections innerJoin SolrConfigs).select(SolrConfigs.columns)
                .where { (SolrCollections.name eq collection) and (SolrCollections.type eq CollectionType.OBJECT) and (SolrCollections.oai eq true)}
                .map { it.toSolr() }
                .firstOrNull()
        }
    }

    /** The [DateFormat] used by this [OaiServer]. */
    private val responseDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    init {
        this.responseDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Handles an OAI-PMH request.
     *
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the OAI-PMH response.
     */
    fun handle(ctx: Context): Document {
        /* Extract parameters. */
        val parameters = when (ctx.method()) {
            HandlerType.GET -> ctx.queryParamMap().map { it.key to it.value.first() }.toMap()
            HandlerType.POST -> ctx.formParamMap().map { it.key to it.value.first() }.toMap()
            else -> return handleError("badArgument", "Unsupported HTTP method.")
        }
        val collection = ctx.pathParam("collection")

        /* Extract OAI verb from query parameters. */
        val verb = parameters["verb"] ?: return handleError("badVerb", "Missing verb.")
        val verbParsed = try {
            valueOf(verb.uppercase())
        } catch (_: IllegalArgumentException) {
            return handleError("badVerb", "Illegal OAI verb '${verb}'.")
        }

        /* Generate response document using OAI server. */
        return when (verbParsed) {
            IDENTIFY -> this.handleIdentify(collection)
            LISTSETS -> this.handleListSets(collection)
            LISTMETADATAFORMATS -> this.handleListMetadataFormats()
            LISTIDENTIFIERS -> this.handleListIdentifiers(collection, parameters)
            LISTRECORDS -> this.handleListRecords(collection, parameters)
            GETRECORD -> this.handleGetRecord(collection, parameters)
        }
    }

    /**
     * Handles a error during OAI-PMH processing.
     *
     * @param code The error code.
     * @param message The error message.
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleError(code: String, message: String): Document  {
        /* Construct response document. */
        val doc = this.documentBuilder.newDocument()

        /* Root element. */
        val root = doc.createElement("OAI-PMH")
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.openarchives.org/OAI/2.0/")
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        root.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd")
        doc.appendChild(root)

        /* Append response date. */
        root.appendChild(doc.createElement("responseDate").apply { this.textContent = this@OaiServer.responseDateFormat.format(Date()) })

        /* Append error element date. */
        root.appendChild(doc.createElement("error").apply {
            this.setAttribute("code", code)
            this.textContent = message
        })
        return doc
    }

    /**
     * Handles the OAI-PMH verb "Identify".
     *
     * @param collection Name of the collection to harvest.
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleIdentify(collection: String): Document {
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
    private fun handleListSets(collection: String): Document {
        val verb = "ListSets"

        /* Prepare facet query. */
        val query = SolrQuery("*:*")
        query.addFacetField(Field.INSTITUTION.solr)
        query.addFacetField(Field.COLLECTION.solr)
        query.addFacetField(Field.PARTIAL_COLLECTION.solr)
        query.rows = 0
        query.start = 0

        /* Execute query. */
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for OAI-PMH.")
        val client = SolrClientProvider.clientForConfig(config)
        val response = client.query(collection, query)

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse(verb)
        for (facet in response.facetFields) {
            for (value in facet.values) {
                if (value.count > 0) {
                    val setElement = root.ownerDocument.createElement("set")
                    setElement.appendChild(root.ownerDocument.createElement("spec").apply { textContent = "${facet.name}:(\"${value.name}\")" })
                    setElement.appendChild(root.ownerDocument.createElement("setName").apply {
                        when (facet.name) {
                            Field.INSTITUTION.solr -> textContent = "Institution: ${value.name}"
                            Field.COLLECTION.solr -> textContent = "Sammlung: ${value.name}"
                            Field.PARTIAL_COLLECTION.solr -> textContent = "Teilsammlung: ${value.name}"
                        }
                    })
                    root.appendChild(setElement)
                }
            }
        }

        return root.ownerDocument
    }

    /**
     * Handles the OAI-PMH verb "ListSets".
     *
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleListMetadataFormats(): Document {
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
     * @param collection Name of the collection to harvest.
     * @param parameters The request parameters.
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleGetRecord(collection: String, parameters: Map<String,String>): Document {
        val identifier = parameters["identifier"] ?: return handleError("badArgument", "Missing identifier.")
        val prefix = parameters["metadataPrefix"] ?: return handleError("badArgument", "Missing metadata prefix.")
        val mapper = Formats.entries.find { it.prefix == prefix }?.toMapper() ?: return handleError("cannotDisseminateFormat", "Unsupported metadata prefix '$prefix'.")

        /* Obtain client and query for entry. */
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for OAI-PMH.")
        val client = SolrClientProvider.clientForConfig(config)
        val response = try {
            client.getById(collection, identifier) ?: return handleError("idDoesNotExist", "The provided identifier '${identifier}' does not exist.")
        } catch (_: Throwable) {
            return handleError("idDoesNotExist", "The provided identifier '${identifier}' does not exist.")
        }

        /* Generate response document. */
        val root = this.documentBuilder.generateResponse("GetRecord", prefix = mapper.format.prefix)
        val doc = root.ownerDocument

        val recordElement = doc.createElement("record")
        root.appendChild(recordElement)

        /* Create header element. */
        val headerElement = doc.createElement("header")
        headerElement.appendChild(doc.createElement("identifier").apply { textContent = response.uuid() })
        headerElement.appendChild(doc.createElement("datestamp").apply { textContent = "2024-01-01" })
        recordElement.appendChild(headerElement)

        /* Map and append metadata. */
        val metadataElement = doc.createElement("metadata")
        mapper.map(metadataElement, response)
        recordElement.appendChild(metadataElement)

        return doc
    }

    /**
     * Handles the OAI-PMH verb "ListRecords".
     *
     * @param collection Name of the collection to harvest.
     * @param parameters The request parameters.
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleListRecords(collection: String, parameters: Map<String,String>): Document {
        /* Parse request. */
        val token = parameters["resumptionToken"]

        /* Determine start, set and mapper to use. */
        val (start, set, mapper) = if (token != null) {
            this.tokens[token] ?: return handleError("badResumptionToken", "Invalid resumption token.")
        } else {
            val prefix = parameters["metadataPrefix"] ?: return handleError("badArgument", "Missing metadata prefix.")
            val mapper = Formats.entries.find { it.prefix == prefix }?.toMapper() ?: return handleError("cannotDisseminateFormat", "Unsupported metadata prefix '$prefix'.")
            Triple(0, parameters["set"], mapper)
        }

        /* Parse dates. */
        val from = parameters["from"]?.let {
            try {
                GRANULARITY_FORMAT.parse(it)
            } catch (_: ParseException) {
                return handleError("badArgument", "Malformed 'from'.")
            }
        }
        val until = parameters["until"]?.let {
            try {
                GRANULARITY_FORMAT.parse(it)
            } catch (_: ParseException) {
                return handleError("badArgument", "Malformed 'until'.")
            }
        }

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        if (from != null && until != null) {
            val from = GRANULARITY_FORMAT.format(from)
            val until = GRANULARITY_FORMAT.format(until)
            query.addFilterQuery("date:[$from TO $until]")
        } else if (from != null) {
            val from = GRANULARITY_FORMAT.format(from)
            query.addFilterQuery("date:[$from TO *]")
        } else if (until != null) {
            val until = GRANULARITY_FORMAT.format(until)
            query.addFilterQuery("date:[* TO $until]")
        }
        if (set != null) {
            query.addFilterQuery(set)
        }

        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for OAI-PMH.")
        val client = SolrClientProvider.clientForConfig(config)
        val response = client.query(collection, query)

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse("ListRecords", prefix = mapper.format.prefix, from = from, until = until, set = set)
        val doc = root.ownerDocument

        /* Process results. */
        for (document in response.results) {
            val recordElement = doc.createElement("record")
            root.appendChild(recordElement)

            val headerElement = doc.createElement("header")
            recordElement.appendChild(headerElement)

            /* Create header element. */
            headerElement.appendChild(doc.createElement("identifier").apply { textContent = document.uuid() })
            headerElement.appendChild(doc.createElement("datestamp").apply { textContent = "2024-01-01" })
            recordElement.appendChild(headerElement)

            /* Map and append metadata. */
            val metadataElement = doc.createElement("metadata")
            mapper.map(metadataElement, document)
            recordElement.appendChild(metadataElement)
        }

        /* If there are more documents to return, include a resumptionToken. */
        val lastElement = start + PAGE_SIZE
        if (response.results.numFound > lastElement) {
            /* Update resumption token. */
            val newToken = UUID.randomUUID().toString()
            this.tokens[newToken] = Triple(lastElement, set, mapper)

            /* Include new token in response. */
            val resumptionTokenElement = doc.createElement("resumptionToken")
            resumptionTokenElement.setAttribute("completeListSize", "${response.results.numFound}")
            resumptionTokenElement.setAttribute("cursor", "$start")
            resumptionTokenElement.appendChild(doc.createTextNode(newToken))
            root.appendChild(resumptionTokenElement)
        }

        /* Store resumption token. */
        return doc
    }

    /**
     * Handles the OAI-PMH verb "ListIdentifiers".
     *
     * @param collection Name of the collection to harvest.
     * @param parameters The request parameters.
     * @return [Document] representing the OAI-PMH response.
     */
    private fun handleListIdentifiers(collection: String, parameters: Map<String,String>): Document {
        /* Parse request. */
        val token = parameters["resumptionToken"]

        /* Determine start, set and mapper to use. */
        val (start, set, mapper) = if (token != null) {
            this.tokens[token] ?: return handleError("badResumptionToken", "Invalid resumption token.")
        } else {
            val prefix = parameters["metadataPrefix"] ?: return handleError("badArgument", "Missing metadata prefix.")
            val mapper = Formats.entries.find { it.prefix == prefix }?.toMapper() ?: return handleError("cannotDisseminateFormat", "Unsupported metadata prefix '$prefix'.")
            Triple(0, parameters["set"], mapper)
        }

        /* Parse optional start and end date. */
        val from = parameters["from"]?.let {
            try {
                GRANULARITY_FORMAT.parse(it)
            } catch (_: ParseException) {
                return handleError("badArgument", "Malformed 'from'.")
            }
        }
        val until = parameters["until"]?.let {
            try {
                GRANULARITY_FORMAT.parse(it)
            } catch (_: ParseException) {
                return handleError("badArgument", "Malformed 'until'.")
            }
        }

        /* Prepare Apache Solr query. */
        val query = SolrQuery("*:*")
        if (from != null && until != null) {
            query.addFilterQuery("date:[${GRANULARITY_FORMAT.format(from)} TO ${GRANULARITY_FORMAT.format(until)}]")
        } else if (from != null) {
            query.addFilterQuery("date:[${GRANULARITY_FORMAT.format(from)} TO *]")
        } else if (until != null) {
            query.addFilterQuery("date:[* TO ${GRANULARITY_FORMAT.format(until)}]")
        }
        if (set != null) {
            query.addFilterQuery(set)
        }
        query.addField("uuid")
        query.start = start
        query.rows = PAGE_SIZE

        /* Execute query. */
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for OAI-PMH.")
        val client = SolrClientProvider.clientForConfig(config)
        val response = client.query(collection, query)
        if (response.results.numFound == 0L) {
            return handleError("noRecordsMatch", "No records match the query.")
        }

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse("ListIdentifiers", prefix = mapper.format.prefix, from = from, until = until, set = set)
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
            this.tokens[newToken] = Triple(lastElement, set, mapper)

            /* Include new token in response. */
            val resumptionTokenElement = doc.createElement("resumptionToken")
            resumptionTokenElement.setAttribute("completeListSize", "${response.results.numFound}")
            resumptionTokenElement.setAttribute("cursor", "$start")
            resumptionTokenElement.appendChild(doc.createTextNode(newToken))
            root.appendChild(resumptionTokenElement)
        }

        /* Store resumption token. */
        return doc
    }

    /**
     * Generates an empty OAI-PMH response document.
     *
     * @param verb The OAI-PMH verb.
     * @return [Pair] of [Document] and root [Element]
     */
    private fun DocumentBuilder.generateResponse(verb: String, prefix: String? = null, set: String? = null, from: Date? = null, until: Date? = null):Element {
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
        responseDate.textContent = this@OaiServer.responseDateFormat.format(Date())
        rootElement.appendChild(responseDate)

        /* Append response date. */
        val request = doc.createElement("request")
        request.setAttribute("verb", verb)
        if (prefix != null) {
            request.setAttribute("metadataPrefix", prefix)
        }
        if (set != null) {
            request.setAttribute("set", set)
        }
        if (from != null) {
            request.setAttribute("from", GRANULARITY_FORMAT.format(from))
        }
        if (until != null) {
            request.setAttribute("until", GRANULARITY_FORMAT.format(until))
        }
        rootElement.appendChild(request)

        /* Verb element. */
        val verbElement = doc.createElement(verb)
        rootElement.appendChild(verbElement)

        return verbElement
    }
}