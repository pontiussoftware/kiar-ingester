package ch.pontius.ingester.config

/**
 * Configuration regarding Apache Solr (for SolrJ).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
class IngestConfig(
    val name: String,

    /** URL of the Apache Solr server. */
    val collection: String,

    /** Password to use when authentication with Apache Solr. */
    val deleteBeforeImport: Boolean = true
)