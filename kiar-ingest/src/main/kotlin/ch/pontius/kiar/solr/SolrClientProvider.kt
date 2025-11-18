package ch.pontius.kiar.solr

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import org.apache.solr.client.solrj.impl.Http2SolrClient
import java.time.Duration

/**
 * A facility that provides [Http2SolrClient]s for a given [ApacheSolrConfig].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object SolrClientProvider {
    /** A cache of [Http2SolrClient]s used by this data ingest server. */
    private val clients = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(12)).removalListener(RemovalListener<String, Http2SolrClient> { key, value, cause -> value?.close() }).build<String, Http2SolrClient>().asMap()

    /**
     * Returns an [Http2SolrClient] for the provided [ApacheSolrConfig].
     *
     * @param config [ApacheSolrConfig] to load [Http2SolrClient] for.
     * @return [Http2SolrClient]
     */
    fun clientForConfig(config: ApacheSolrConfig): Http2SolrClient =  this.clients.computeIfAbsent(config.name) {
        /* Prepare builder */
        var httpBuilder = Http2SolrClient.Builder(config.server)
        if (config.username != null && config.password != null) {
            httpBuilder = httpBuilder.withBasicAuthCredentials(config.username, config.password)
        }
        /* Prepare Apache Solr client. */
        httpBuilder.build()
    }
}