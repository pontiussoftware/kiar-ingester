package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.config.SolrConfig
import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.solrj.Constants
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_CANTON
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_OUTPUT
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException

import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.common.SolrInputDocument
import java.io.IOException
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
        /** Prepare [ConcurrentUpdateHttp2SolrClient] that performs Apache Solr Updates. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.user != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.user, this.config.password)
        }
        val client = ConcurrentUpdateHttp2SolrClient.Builder(this.config.server, httpBuilder.build(), true).build()
        client.use {
            /* Prepare all collections for import. */
            this.prepareIngest(client)

            /* Consume flow and commit (or rollback)*/
            runBlocking {
                this@ApacheSolrSink.input.toFlow().collect {

                    it.addField(FIELD_NAME_PARTICIPANT, this@ApacheSolrSink.context)
                    if (it[FIELD_NAME_CANTON]?.value == "BE") {  /* TODO: This is a hack! */
                        it.addField(FIELD_NAME_OUTPUT, "mmBE Inventar")
                    }

                    for (c in this@ApacheSolrSink.config.collections) {
                        if (c.isMatch(it)) {
                            try {
                                val response = client.add(c.name, it)
                                if (response.status == 0) {
                                    LOGGER.info("Successfully added document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                                } else {
                                    LOGGER.warn("Error while adding document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                                }
                            } catch (e: SolrServerException) {
                                LOGGER.warn("Server reported error while adding document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                            }
                        }
                    }
                }
            }

            /* Finalize ingest for all collections. */
            this.finalizeIngest(client)
        }
    }

    /* Purge all collections that were configured. */
    private fun prepareIngest(client: ConcurrentUpdateHttp2SolrClient) {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            if (c.deleteBeforeImport) {
                val response = client.deleteByQuery(c.name, "$FIELD_NAME_PARTICIPANT:\"${this.context}\"")
                if (response.status != 0) {
                    throw IllegalArgumentException("Data ingest (name = ${this.context}, collection = ${c.name}) failed because delete before import could not be executed.")
                }
            }
        }
    }

    /* Purge all collections that were configured. */
    private fun finalizeIngest(client: ConcurrentUpdateHttp2SolrClient) {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.context}, collection = ${c.name}) completed; committing...")
            try {
                val response = client.commit(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
                }
            } catch (e: SolrServerException) {
                client.rollback(c.name)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}). Rolling back...")
            } catch (e: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
            }
        }
    }
}