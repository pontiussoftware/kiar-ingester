package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.ingester.config.SolrConfig
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_CANTON
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_OUTPUT
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_PARTICIPANT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrInputDocument
import java.io.Closeable
import java.io.IOException
import java.lang.IllegalStateException


/**
 * A [Sink] that processes [SolrInputDocument]s and ingests them into Apache Solr.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ApacheSolrSink(override val input: Source<SolrInputDocument>, private val config: SolrConfig): Sink<SolrInputDocument>, Closeable {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The [Http2SolrClient] used to interact with Apache Solr.*/
    private val client: Http2SolrClient

    /** [ApacheSolrField] for the different collections. */
    private val validators = HashMap<String,List<ApacheSolrField>>()

    init {
        /* Prepare HTTP client builder. */
        var httpBuilder = Http2SolrClient.Builder(this.config.server)
        if (this.config.user != null && this.config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(this.config.user, this.config.password)
        }
        this.client = httpBuilder.build()

        /* Initializes the document validators. */
        this.initializeValidators()
    }

    /**
     * Creates and returns a [Flow] for this [ApacheSolrSink].
     *
     * @return [Flow]
     */
    override fun execute() = runBlocking {
        /* Prepare collection for data ingest. */
        this@ApacheSolrSink.prepareIngest()

        /* Consume incoming flow of documents. */
        this@ApacheSolrSink.input.toFlow().collect { doc ->
            val uuid = doc[Constants.FIELD_NAME_UUID]
            try {
                LOGGER.debug("Incoming document (name = ${this@ApacheSolrSink.context}, uuid = $uuid).")

                doc.addField(FIELD_NAME_PARTICIPANT, this@ApacheSolrSink.context)
                if (doc[FIELD_NAME_CANTON]?.value == "BE") {  /* TODO: This is a hack! */
                    doc.addField(FIELD_NAME_OUTPUT, "mmBE Inventar")
                }

                LOGGER.debug("Starting document ingest (name = ${this@ApacheSolrSink.context}, uuid = $uuid).")
                for (c in this@ApacheSolrSink.config.collections) {
                    try {
                        if (c.isMatch(doc) && this@ApacheSolrSink.validate(c.name, doc)) {
                            val response = this@ApacheSolrSink.client.add(c.name, doc)
                            if (response.status == 0) {
                                LOGGER.info("Successfully added document (name = ${this@ApacheSolrSink.context}, uuid = $uuid, collection = ${c.name}).")
                            } else {
                                LOGGER.warn("Error while adding document (name = ${this@ApacheSolrSink.context}, uuid = $uuid, collection = ${c.name}).")
                            }
                        }
                    } catch (e: SolrServerException) {
                        LOGGER.warn("Server reported error while adding document (name = ${this@ApacheSolrSink.context}, uuid = $uuid, collection = ${c.name}).")
                    }
                }
            } catch (e: Throwable) {
                LOGGER.error("Serious error occurred while adding a document (name = ${this@ApacheSolrSink.context}, uuid = $uuid): $e")
            }
        }

        /* Finalize ingest for all collections. */
        this@ApacheSolrSink.finalizeIngest()
    }

    /**
     * Initializes the [ApacheSolrField]s for the different collections.
     */
    private fun initializeValidators() {
        for (c in this.config.collections) {
            /* Prepare HTTP client builder. */
            val fields = SchemaRequest.Fields().process(this.client, c.name).fields
            val validators = fields.map { schemaField ->
                ApacheSolrField(schemaField["name"] as String, schemaField["required"] as Boolean, schemaField["multiValued"] as Boolean)
            }
            this.validators[c.name] = validators
        }
    }

    /**
     * Validates the provided [SolrInputDocument]
     *
     * @param collection The name of the collection to validate the [SolrInputDocument] for.
     * @param doc The [SolrInputDocument] to validate.
     * @return True on successful validation, false otherwise.
     */
    private fun validate(collection: String, doc: SolrInputDocument): Boolean {
        val validators = this.validators[collection] ?: throw IllegalStateException("No validators for collection ${collection}. This is a programmer's error!")
        for (v in validators) {
            if (!v.isValid(doc)) {
                LOGGER.warn("Error while validating document (name = ${this@ApacheSolrSink.context}, uuid = ${doc[Constants.FIELD_NAME_UUID]}, collection = ${collection}): ${v.isInvalidReason(doc)}")
                return false
            }
        }
        return true
    }

    /** Purge all collections that were configured. */
    private fun prepareIngest() {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            if (c.deleteBeforeImport) {
                LOGGER.info("Purging collection (name = ${this@ApacheSolrSink.context} collection = ${c.name}).")
                val response = this.client.deleteByQuery(c.name, "$FIELD_NAME_PARTICIPANT:\"${this.context}\"")
                if (response.status != 0) {
                    LOGGER.error("Purge of collection failed (name = ${this@ApacheSolrSink.context} collection = ${c.name}). Aborting...")
                    throw IllegalArgumentException("Data ingest (name = ${this.context}, collection = ${c.name}) failed because delete before import could not be executed.")
                }
                LOGGER.info("Purge of collection successful (name = ${this@ApacheSolrSink.context} collection = ${c.name}).")
            }
        }
    }

    /* Purge all collections that were configured. */
    private fun finalizeIngest() {
        /* Purge all collections that were configured. */
        for (c in this.config.collections) {
            LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.context}, collection = ${c.name}) completed; committing...")
            try {
                val response = this.client.commit(c.name)
                if (response.status == 0) {
                    LOGGER.info("Data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}) committed successfully.")
                } else {
                    LOGGER.warn("Failed to commit data ingest (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
                }
            } catch (e: SolrServerException) {
                this.client.rollback(c.name)
                LOGGER.error("Failed to commit data ingest due to server error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}). Rolling back...")
            } catch (e: IOException) {
                LOGGER.error("Failed to commit data ingest due to IO error (name = ${this@ApacheSolrSink.config.name}, collection = ${c.name}).")
            }
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}