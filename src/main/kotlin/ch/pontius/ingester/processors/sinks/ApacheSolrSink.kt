package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.config.SolrConfig
import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_CANTON
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager

import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument
import java.lang.IllegalArgumentException

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val config: SolrConfig): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun execute() {
        /* If configured, then delete before import. */
        val collection = this.config.collection

        /** Prepare [ConcurrentUpdateHttp2SolrClient] that performs Apache Solr Updates. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.user != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.user, this.config.password)
        }
        val client = ConcurrentUpdateHttp2SolrClient.Builder(this.config.server, httpBuilder.build(), true).build()
        client.use {
            if (this.config.deleteBeforeImport) {
                val response = client.deleteByQuery(collection, "$FIELD_NAME_PARTICIPANT:\"${this.config.name}\"")
                if (response.status != 0) {
                    throw IllegalArgumentException("Data ingest failed because delete before import could not be executed.")
                }
            }

            /* Consume flow and commit (or rollback)*/
            try {
                runBlocking {
                    this@ApacheSolrSink.input.toFlow().collect {
                        it.addField(FIELD_NAME_PARTICIPANT, this@ApacheSolrSink.config.name)
                        it.addField(FIELD_NAME_CANTON, "BE")
                        it.addField("_output_", "all")
                        it.addField("_display_", "test")
                        client.add(collection, it)
                    }
                }
                LOGGER.debug("Data ingest ${this@ApacheSolrSink.config.name} (collection = ${this@ApacheSolrSink.config.collection}) successful; committing...")
                client.commit(collection)
            } catch (e: Throwable) {
                client.rollback(collection)
            }
        }
    }
}