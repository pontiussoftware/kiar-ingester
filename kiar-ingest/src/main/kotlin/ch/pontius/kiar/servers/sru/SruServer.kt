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
import org.apache.solr.client.solrj.request.SolrQuery
import org.apache.solr.client.solrj.util.ClientUtils
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
 * @version 1.0.1
 */
class SruServer {

    companion object {
        /** The maximum number of records returned per SRU request. */
        const val MAX_RECORDS = 100

        /** The Solr full-text field (a copy field on the Solr side) that SRU queries run against. */
        private const val FULLTEXT_FIELD = "_fulltext_"

        /** Splits a query into double-quoted phrases (group 1) and single terms (group 2). */
        private val TERM_REGEX = Regex("\"([^\"]*)\"|(\\S+)")
    }

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
     * @param collection The name of the collection to search.
     * @param query The query string.
     * @param pageSize The maximum number of records to return.
     * @param startRecord The (1-based) index of the first record to return.
     * @return [Document] representing the SRU response.
     */
    fun handle(collection: String, query: String, pageSize: Int, startRecord: Int): Document {
        /* Read associated config. */
        val config = this.collections[collection] ?: throw IllegalArgumentException("Collection '$collection' not found or not configured for SRU.")

        /* Clamp paging parameters; both are client-controlled. SRU's startRecord is 1-based, Solr's start is 0-based. */
        val rows = pageSize.coerceIn(1, MAX_RECORDS)
        val startRecord = startRecord.coerceAtLeast(1)

        val response = try {
            /* Prepare Apache Solr query. */
            val solrQuery = SolrQuery(toSolrQuery(query))
            solrQuery.start = startRecord - 1
            solrQuery.rows = rows

            /* Prepare client. */
            val client = SolrClientProvider.clientForConfig(config)
            val response = client.query(collection, solrQuery)

            /* Prepare response. */
            val root = this.documentBuilder.generateResponse(response.results.numFound)

            /* Process results. */
            for ((index, document) in response.results.withIndex()) {
                val recordElement = root.ownerDocument.createElement("zs:record")
                recordElement.appendChild(root.ownerDocument.createElement("zs:recordPosition").apply { textContent = (startRecord + index).toString() })
                root.appendChild(recordElement)

                /* Map and append metadata. */
                val metadataElement = root.ownerDocument.createElement("zs:recordData")
                DCMapper.map(metadataElement, document)
                recordElement.appendChild(metadataElement)
            }
            root
        } catch (e: Throwable) {
            logger.error(e) { "Error processing SRU request for collection '$collection' (q = $query): ${e.message}" }
            this.documentBuilder.generateResponse(0)
        }

        /* Return document. */
        return response.ownerDocument
    }

    /**
     * Converts a client-supplied SRU query string into a safe Apache Solr query against the full-text field.
     *
     * The input is treated as search terms only: it is split into whitespace-separated terms and double-quoted phrases,
     * and every term is escaped so that no Solr query syntax (field selectors, local parameters, ranges, wildcards) can
     * be injected. A blank query or a lone '*' matches all documents.
     *
     * @param query The raw query string.
     * @return A Solr query string.
     */
    internal fun toSolrQuery(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed == "*") return "*:*"
        val terms = TERM_REGEX.findAll(trimmed).mapNotNull { match ->
            val phrase = match.groups[1]?.value
            val term = match.groups[2]?.value
            when {
                phrase != null -> phrase.trim().takeIf { it.isNotEmpty() }?.let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
                term != null -> ClientUtils.escapeQueryChars(term)
                else -> null
            }
        }.toList()
        if (terms.isEmpty()) return "*:*"
        return "$FULLTEXT_FIELD:(${terms.joinToString(" ")})"
    }

    /**
     * Generates an empty SRU response document.
     *
     * @param numHits The total number of hits in the response.
     * @return [Pair] of [Document] and root [Element]
     */
    private fun DocumentBuilder.generateResponse(numHits: Long): Element {
        /* Construct response document. */
        val doc = this.newDocument()

        /* Root element. */
        val rootElement = doc.createElement("zs:searchRetrieveResponse")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:zs", "http://www.loc.gov/zing/srw/")
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", "http://purl.org/dc/elements/1.1/")
        doc.appendChild(rootElement)

        /* Append number of records. */
        rootElement.appendChild(doc.createElement("zs:numberOfRecords").apply { textContent = numHits.toString() })
        rootElement.appendChild(doc.createElement("zs:version").apply { textContent = "1.2" })

        /* Append response date. */
        val records = doc.createElement("zs:records")
        rootElement.appendChild(records)
        return records
    }
}