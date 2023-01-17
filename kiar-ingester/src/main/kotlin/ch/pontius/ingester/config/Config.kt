package ch.pontius.ingester.config

import kotlinx.serialization.Serializable

/**
 * Configuration file for Ingester Pipeline.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Config(
    /** A [List] of registered [SolrConfig] instances. */
    val solr: List<SolrConfig> = emptyList(),

    /** A [List] of registered [MappingConfig] instances. */
    val mappers: List<MappingConfig> = emptyList(),

    /** A [List] of registered [JobConfig] instances. */
    val jobs: List<JobConfig> = emptyList()
)