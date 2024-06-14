package ch.pontius.kiar.oai.mapper

import org.apache.solr.common.SolrDocument
import org.w3c.dom.Node

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface OAIMapper {


    /**
     *
     */
    fun map(appendTo: Node, document: SolrDocument)
}