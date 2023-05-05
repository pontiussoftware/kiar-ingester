package ch.pontius.kiar.config

import ch.pontius.kiar.utilities.serialization.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Configuration file for Ingester Pipeline.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Config(
    /** Flag indicating whether the CLI should be started. */
    val cli: Boolean = true,

    /** Flag indicating whether the web server should be started. */
    val web: Boolean = true,

    /** The port to start the web server under.*/
    val webPort: Int = 7070,

    /** Path to database folder. */
    @Serializable(with = PathSerializer::class)
    val dbPath: Path,

    /** Path to ingest main folder. */
    @Serializable(with = PathSerializer::class)
    val ingestPath: Path,

    /** Path to log file. */
    @Serializable(with = PathSerializer::class)
    val logPath: Path,

    /** A [List] of registered [SolrConfig] instances. */
    val solr: List<SolrConfig> = emptyList(),

    /** A [List] of registered [MappingConfig] instances. */
    val mappers: List<MappingConfig> = emptyList(),

    /** A [List] of registered [JobConfig] instances. */
    val jobs: List<JobConfig> = emptyList()
)