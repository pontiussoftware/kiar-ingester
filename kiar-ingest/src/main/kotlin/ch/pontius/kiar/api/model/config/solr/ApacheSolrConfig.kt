package ch.pontius.kiar.api.model.config.solr

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import kotlinx.serialization.Serializable

typealias SolrConfigId = Int

/**
 * Configuration regarding Apache Solr (for SolrJ).
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class ApacheSolrConfig(
    /** The (optional) database [SolrConfigId] of this [ApacheSolrConfig]. */
    val id: SolrConfigId? = null,

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

    /** A list of [ApacheSolrCollection] for this [ApacheSolrConfig]. */
    val collections: List<ApacheSolrCollection> = emptyList(),

    /** A list of [ImageDeployment] for this [ApacheSolrConfig]. */
    val deployments: List<ImageDeployment> = emptyList()
)