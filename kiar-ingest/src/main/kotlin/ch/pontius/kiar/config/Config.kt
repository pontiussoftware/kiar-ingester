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

    /** Number of day to retain job logs. */
    val jobLogRetentionDays: Int = 30,

    /** Number of input files to retain. */
    val inputRetentionCount: Int = 1,

    /**
     * Whether the session cookie is marked 'Secure' (only sent over HTTPS). Enable this whenever the dashboard is served
     * over TLS (typically behind a reverse proxy); leave it disabled only for plain-HTTP development setups.
     */
    val secureCookies: Boolean = false,

    /**
     * Origins (e.g. "http://localhost:4200") that may call the API cross-origin with credentials. The dashboard is served
     * from the same origin as the API, so this is only needed for development (e.g. the Angular dev server). When empty,
     * CORS is disabled entirely.
     */
    val allowedOrigins: List<String> = emptyList()
)