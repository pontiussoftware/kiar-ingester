package ch.pontius.kiar.solr

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * A facility that provides [HttpJettySolrClient]s for a given [ApacheSolrConfig].
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
object SolrClientProvider {

    /** Time to wait for a TCP connection to Apache Solr. */
    private const val CONNECTION_TIMEOUT_SECONDS = 10L

    /** Time a connection may stay idle (no bytes received) before a request is aborted. */
    private const val IDLE_TIMEOUT_SECONDS = 120L

    /** Maximum total duration of a single request. Long enough for large commits, short enough not to pin threads forever. */
    private const val REQUEST_TIMEOUT_SECONDS = 600L

    /**
     * A cache of shared [HttpJettySolrClient]s. Keyed by the configuration's connection fingerprint, so a changed server URL
     * or rotated credentials produce a new client; the old one is closed when it expires.
     */
    private val clients = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .maximumSize(64)
        .removalListener(RemovalListener<String, HttpJettySolrClient> { _, value, _ -> value?.close() })
        .build<String, HttpJettySolrClient>()
        .asMap()

    /**
     * Returns a shared [HttpJettySolrClient] for the provided [ApacheSolrConfig]. The client must not be closed by the caller.
     *
     * @param config [ApacheSolrConfig] to load [HttpJettySolrClient] for.
     * @return [HttpJettySolrClient]
     */
    fun clientForConfig(config: ApacheSolrConfig): HttpJettySolrClient = this.clients.computeIfAbsent(this.fingerprint(config)) {
        this.newClient(config)
    }

    /**
     * Creates a new, dedicated [HttpJettySolrClient] for the provided [ApacheSolrConfig] with the standard timeouts and
     * credentials applied. The caller owns the client and must close it.
     *
     * @param config [ApacheSolrConfig] to create the [HttpJettySolrClient] for.
     * @return [HttpJettySolrClient]
     */
    fun newClient(config: ApacheSolrConfig): HttpJettySolrClient {
        var builder = HttpJettySolrClient.Builder(config.server)
            .withConnectionTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withIdleTimeout(IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withRequestTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (config.username != null && config.password != null) {
            builder = builder.withBasicAuthCredentials(config.username, config.password)
        }
        return builder.build()
    }

    /**
     * Derives the cache key for a configuration from the properties that affect the connection.
     */
    private fun fingerprint(config: ApacheSolrConfig): String = "${config.id}|${config.server}|${config.username}|${config.password?.hashCode()}"
}
