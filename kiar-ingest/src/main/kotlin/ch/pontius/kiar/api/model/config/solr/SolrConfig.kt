package ch.pontius.kiar.api.model.config.solr

import ch.pontius.kiar.config.CollectionConfig
import kotlinx.serialization.Serializable

/**
 * Configuration regarding Apache Solr (for SolrJ).
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
class SolrConfig(
    /** The (optional) database ID of this [SolrConfig]. */
    val id: String? = null,

    /** The name of this [SolrConfig]. */
    val name: String,

    /** An optional description  of this [SolrConfig]. */
    val description: String? = null,

    /** URL of the Apache Solr server. */
    val server: String = "http://localhost:8983",

    /** Username to use when authenticating with Apache Solr. */
    val user: String? = null,

    /** Password to use when authentication with Apache Solr. */
    val password: String? = null,

    /** A list of [CollectionConfig] for this [SolrConfig]. */
    val collections: List<CollectionConfig> = emptyList()
)