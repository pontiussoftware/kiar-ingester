package ch.pontius.kiar.solr

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient
import java.time.Duration

/**
 * A facility that provides [HttpJettySolrClient]s for a given [ApacheSolrConfig].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
object SolrClientProvider {
    /** A cache of [HttpJettySolrClient]s used by this data ingest server. */
    private val clients = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).removalListener(RemovalListener<String, HttpJettySolrClient> { key, value, cause -> value?.close() }).build<String, HttpJettySolrClient>().asMap()

    /**
     * Returns an [HttpJettySolrClient] for the provided [ApacheSolrConfig].
     *
     * @param config [ApacheSolrConfig] to load [HttpJettySolrClient] for.
     * @return [HttpJettySolrClient]
     */
    fun clientForConfig(config: ApacheSolrConfig): HttpJettySolrClient =  this.clients.computeIfAbsent(config.name) {
        /* Prepare builder */
        var httpBuilder = HttpJettySolrClient.Builder(config.server)
        if (config.username != null && config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
        }
        /* Prepare Apache Solr client. */
        httpBuilder.build()
    }
}