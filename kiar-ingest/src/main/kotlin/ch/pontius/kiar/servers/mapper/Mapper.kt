package ch.pontius.kiar.servers.mapper

import ch.pontius.kiar.servers.mapper.Formats
import org.apache.solr.common.SolrDocument
import org.w3c.dom.Node

/**
 * [Mapper] maps a [SolrDocument] to an XML [Node] to generate an OAI-PMH record.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Mapper {
    /** The [Formats] supported by this [Mapper]. */
    val format: Formats

    /**
     * Maps the given [SolrDocument] to an XML [Node] and appends it to the given [Node].
     *
     * @param appendTo The [Node] to which the generated XML [Node] should be appended.
     * @param document The [SolrDocument] that should be mapped.
     */
    fun map(appendTo: Node, document: SolrDocument)
}