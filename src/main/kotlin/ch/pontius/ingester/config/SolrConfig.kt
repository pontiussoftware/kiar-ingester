package ch.pontius.ingester.config

/**
 * Configuration regarding Apache Solr (for SolrJ).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
class SolrConfig(
    val name: String,

    /** URL of the Apache Solr server. */
    val server:  String = "http://localhost:8983",

    /** Username to use when authenticating with Apache Solr. */
    val user: String? = null,

    /** Password to use when authentication with Apache Solr. */
    val password: String? = null,

    /** URL of the Apache Solr server. */
    val collection: String,

    /** Password to use when authentication with Apache Solr. */
    val deleteBeforeImport: Boolean = true
)