package ch.pontius.kiar.api.model.config.solr

import ch.pontius.kiar.api.model.job.Job
import ch.pontius.kiar.config.CollectionConfig
import kotlinx.serialization.Serializable

/**
 * Configuration regarding Apache Solr (for SolrJ).
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
class ApacheSolrConfig(
    /** The (optional) database ID of this [ApacheSolrConfig]. */
    val id: String? = null,

    /** The name of this [ApacheSolrConfig]. */
    val name: String,

    /** An optional description  of this [ApacheSolrConfig]. */
    val description: String? = null,

    /** URL of the Apache Solr server. */
    val server: String = "http://localhost:8983",

    /** Public URL of the Apache Solr server. Can be null.*/
    val publicServer: String? = null,

    /** Username to use when authenticating with Apache Solr. */
    val username: String? = null,

    /** Password to use when authentication with Apache Solr. */
    val password: String? = null,

    /** Timestamp of this [ApacheSolrConfig]'s creation. */
    val createdAt: Long? = null,

    /** Timestamp of this [ApacheSolrConfig]'s last change. */
    val changedAt: Long? = null,

    /** A list of [CollectionConfig] for this [ApacheSolrConfig]. */
    val collections: List<ApacheSolrCollection> = emptyList()
)