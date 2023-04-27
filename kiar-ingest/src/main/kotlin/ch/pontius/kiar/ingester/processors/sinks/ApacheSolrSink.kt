package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.ingester.config.SolrConfig
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_CANTON
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_OUTPUT
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT

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
            val flow = this@ApacheSolrSink.input.toFlow()
            this.prepareIngest(client)

            /* Consume flow and commit (or rollback)*/
            runBlocking {
                flow.collect {
                    try {
                        LOGGER.debug("Incoming document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}).")

                        it.addField(FIELD_NAME_PARTICIPANT, this@ApacheSolrSink.context)
                        if (it[FIELD_NAME_CANTON]?.value == "BE") {  /* TODO: This is a hack! */
                            it.addField(FIELD_NAME_OUTPUT, "mmBE Inventar")
                        }

                        LOGGER.debug("Starting document ingest (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}).")
                        for (c in this@ApacheSolrSink.config.collections) {
                            try {
                                if (c.isMatch(it)) {
                                    LOGGER.debug("Adding document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                                    val response = client.add(c.name, it)
                                    if (response.status == 0) {
                                        LOGGER.info("Successfully added document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                                    } else {
                                        LOGGER.warn("Error while adding document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                                    }

                                } else {
                                    LOGGER.debug("Document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}) no match for collection.")
                                }
                            } catch (e: SolrServerException) {
                                LOGGER.warn("Server reported error while adding document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}, collection = ${c.name}).")
                            }
                        }
                    } catch (e: Throwable) {
                        LOGGER.error("Serious error occurred while adding a document (name = ${this@ApacheSolrSink.context}, uuid = ${it[Constants.FIELD_NAME_UUID]}): $e")
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
                LOGGER.info("Purging collection (name = ${this@ApacheSolrSink.context} collection = ${c.name}).")
                val response = client.deleteByQuery(c.name, "$FIELD_NAME_PARTICIPANT:\"${this.context}\"")
                if (response.status != 0) {
                    LOGGER.error("Purge of collection failed (name = ${this@ApacheSolrSink.context} collection = ${c.name}). Aborting...")
                    throw IllegalArgumentException("Data ingest (name = ${this.context}, collection = ${c.name}) failed because delete before import could not be executed.")
                }
                LOGGER.info("Purge of collection successful (name = ${this@ApacheSolrSink.context} collection = ${c.name}).")
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