package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.processors.sources.Source
import org.apache.solr.common.SolrInputDocument

/**
 * List of all [Sink]s.
 *
 * @author Ralph Gasser
 * @version 1.0.
 */
enum class Sinks {
    SOLR, LOGGER;
}