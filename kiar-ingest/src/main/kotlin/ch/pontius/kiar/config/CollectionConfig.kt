package ch.pontius.kiar.config

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class CollectionConfig (
    /** The name of the Apache Solr collection. */
    val name: String,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val filter: List<String> = emptyList(),

    /** Flag indicating, that  collection should be purged before starting an import. */
    val deleteBeforeImport: Boolean = true,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val acceptEmptyFilter: Boolean = false
)