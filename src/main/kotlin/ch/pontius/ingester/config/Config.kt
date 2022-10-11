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
    /** URL of the Apache Solr server. */
    val server:  String = "http://localhost:8983",

    /** Username to use when authenticating with Apache Solr. */
    val user: String? = null,

    /** Password to use when authentication with Apache Solr. */
    val password: String? = null,

    /** A [List] of registered [MappingConfig] instances. */
    val mapper: List<MappingConfig> = emptyList(),

    /** A [List] of registered [ImageConfig] instances. */
    val image: List<ImageConfig> = emptyList(),

    /** A [List] of registered [IngestConfig] instances. */
    val ingest: List<IngestConfig> = emptyList(),

    /** A [List] of registered [JobConfig] instances. */
    val jobs: List<JobConfig> = emptyList()
)