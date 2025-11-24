package ch.pontius.kiar.servers.sru

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.SolrConfigs.toSolr
import ch.pontius.kiar.ingester.parsing.xml.XmlDocumentParser
import ch.pontius.kiar.servers.mapper.DCMapper
import ch.pontius.kiar.solr.SolrClientProvider
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.http.Context
import org.apache.solr.client.solrj.SolrQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.time.Duration
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/** The [KLogger] instance for [SruServer]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A simple SRU (Search / Retrieval via URL) server.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SruServer {
    /** The [DocumentBuilder] instance used by this [XmlDocumentParser]. */
    private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** A cache of [ApacheSolrConfig]s used by this [SruServer]. */
    private val collections = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).build<String, ApacheSolrConfig?> { collection ->
        transaction {
            (SolrCollections innerJoin SolrConfigs).select(SolrConfigs.columns)
                .where { (SolrCollections.name eq collection) and (SolrCollections.type eq CollectionType.OBJECT) and (SolrCollections.sru eq true)}
                .map { it.toSolr() }
                .firstOrNull()
        }
    }

    /**
     * Handles a SRU request.
     *
     * @param ctx The Javalin [Context] object
     * @return [Document] representing the SRU response.
     */
    fun handle(ctx: Context): Document {
        /* Read collection and associated config. */
        val collection = ctx.pathParam("collection")
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for SRU.")

        /* Construct response document. */
        val root = this.documentBuilder.generateResponse()
        val doc = root.ownerDocument

        /* Read remaining parameters. */
        val query = ctx.queryParam("query") ?: "*"
        val pageSize = ctx.queryParam("maximumRecords")?.toIntOrNull() ?: 100
        val pageIndex = ctx.queryParam("startRecord")?.toIntOrNull() ?: 1

        try {
            /* Prepare Apache Solr query. */
            val solrQuery = SolrQuery("_fulltext_:$query")
            solrQuery.start = pageIndex
            solrQuery.rows = pageSize

            /* Prepare client. */
            val client = SolrClientProvider.clientForConfig(config)
            val response = client.query(collection, solrQuery)

            /* Process results. */
            for (document in response.results) {
                val recordElement = doc.createElement("zs:record")
                root.appendChild(recordElement)

                /* Map and append metadata. */
                val metadataElement = doc.createElement("zs:recordData")
                DCMapper.map(metadataElement, document)
                recordElement.appendChild(metadataElement)
            }

        } catch (e: Throwable) {
            logger.error(e) { "Error processing SRU request for collection '$collection' (q = $query): ${e.message}" }
        }

        /* Return document. */
        return doc
    }

    /**
     * Generates an empty SRU response document.
     *
     * @return [Pair] of [Document] and root [Element]
     */
    private fun DocumentBuilder.generateResponse(): Element {
        /* Construct response document. */
        val doc = this.newDocument()

        /* Root element. */
        val rootElement = doc.createElement("zs:searchRetrieveResponse")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:zs", "http://www.loc.gov/zing/srw/")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", "http://purl.org/dc/elements/1.1/")
        doc.appendChild(rootElement)

        /* Append response date. */
        val records = doc.createElement("zs:records")
        rootElement.appendChild(records)
        return records
    }
}