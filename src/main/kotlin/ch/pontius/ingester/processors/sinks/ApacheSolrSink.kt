package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.config.IngestConfig
import ch.pontius.ingester.processors.sources.Source
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_CANTON
import ch.pontius.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT

import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager

import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient
import org.apache.solr.common.SolrInputDocument

/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val client: ConcurrentUpdateHttp2SolrClient, private val config: IngestConfig): Sink<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun execute(): Flow<Unit> {
        /* If configured, then delete before import. */
        val collection = this.config.collection
        if (this.config.deleteBeforeImport) {
            try {
                val response = this@ApacheSolrSink.client.deleteByQuery(collection, "$FIELD_NAME_PARTICIPANT:\"${this.config.name}\"")
                if (response.status != 0) {
                    return emptyFlow()
                }
            } catch (e: Throwable) {
                return emptyFlow()
            }
        }

        /* Generate flow that adds the individual documents. */
        val flow = this.input.toFlow().onEach {
            it.addField(FIELD_NAME_PARTICIPANT, this.config.name)
            it.addField(FIELD_NAME_CANTON, "BE")
            it.addField("_output_", "all")
            it.addField("_display_", "test")

            this@ApacheSolrSink.client.add(collection, it)
        }.onCompletion {
            if (it == null) {
                LOGGER.info("Data ingest ${this@ApacheSolrSink.config.name} (collection = ${this@ApacheSolrSink.config.collection}) successful; committing.")
                try {
                    this@ApacheSolrSink.client.commit(collection)
                } catch (e: Throwable) {
                    LOGGER.error("Data ingest ${this@ApacheSolrSink.config.name} (collection = ${this@ApacheSolrSink.config.collection}) commit failed: ${e.message}.")
                }
            } else {
                LOGGER.error("Data ingest ${this@ApacheSolrSink.config.name} (collection = ${this@ApacheSolrSink.config.collection}) failed ${it.message}. Rolling back.")
                this@ApacheSolrSink.client.rollback(collection)
            }
        }

        return flow { flow.collect() }
    }
}