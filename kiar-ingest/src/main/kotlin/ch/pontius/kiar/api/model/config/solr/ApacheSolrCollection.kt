package ch.pontius.kiar.api.model.config.solr

import kotlinx.serialization.Serializable

typealias SolrCollectionId = Int

/**
 * Configuration regarding an Apache Solr collection.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
@Serializable
data class ApacheSolrCollection(
    /** Optional [SolrCollectionId] of the [ApacheSolrCollection] entry. */
    val id: SolrCollectionId? = null,

    /** The name of the Apache Solr collection. */
    val name: String,

    /** The display name of the Apache Solr collection. */
    val displayName: String?,

    /** The type of [ApacheSolrCollection]. */
    val type: CollectionType,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val selector: String?,

    /** Flag indicating, that [ApacheSolrCollection] can be harvested via OAI-PMH. */
    val oai: Boolean = false,

    /** Flag indicating, that [ApacheSolrCollection] can be queried via SRU. */
    val sru: Boolean = false,

    /** Flag indicating, that [ApacheSolrCollection] should be purged before starting an import. */
    val deleteBeforeIngest: Boolean = true
)