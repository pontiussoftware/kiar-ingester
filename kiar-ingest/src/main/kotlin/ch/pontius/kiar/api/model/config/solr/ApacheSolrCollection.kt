package ch.pontius.kiar.api.model.config.solr

import kotlinx.serialization.Serializable

/**
 * Configuration regarding an Apache Solr collection.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ApacheSolrCollection(
    /** The name of the Apache Solr collection. */
    val name: String,

    /** The display name of the Apache Solr collection. */
    val displayName: String?,

    /** The type of [ApacheSolrCollection]. */
    val type: CollectionType,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val selector: String?,

    /** Flag indicating, that  collection should be purged before starting an import. */
    val deleteBeforeImport: Boolean = true,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val acceptEmptyFilter: Boolean = false
)