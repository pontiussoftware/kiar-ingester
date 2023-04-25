package ch.pontius.kiar.ingester.ingest

import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Ingester(val server: String, val collection: String) {

    /** The [ConcurrentUpdateHttp2SolrClient] used for data ingest into Apache Solr. */
    private val client: ConcurrentUpdateHttp2SolrClient

    init {
        val client = Http2SolrClient.Builder(server).build()
        this.client = ConcurrentUpdateHttp2SolrClient.Builder(this.server, client).build()
    }


    /**
     *
     */
    fun add(doc: SolrInputDocument) = this.client.add(this.collection, doc).status == 200

    /**
     *
     */
    fun addAll(docs: Iterator<SolrInputDocument>) = this.client.add(this.collection, docs)

}