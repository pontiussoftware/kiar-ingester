package ch.pontius.kiar.ingester.parsing.xml

import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import org.apache.solr.common.SolrInputDocument
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression

/**
 * Parses a XML [Node] into [SolrInputDocument].
 *
 * @param node The [Node] to parse.
 * @return [SolrInputDocument]
 */
fun List<Pair<ValueParser<*>, XPathExpression>>.parse(node: Node, context: ProcessingContext): SolrInputDocument {
    val doc = SolrInputDocument()
    for ((parser, expr) in this) {
        val nl = expr.evaluate(node, XPathConstants.NODESET) as? NodeList
        if (nl != null) {
            for (i in 0 until nl.length) {
                val value = nl.item(i).nodeValue
                if (!value.isNullOrBlank()) {
                    parser.parse(nl.item(i).nodeValue, doc, context)
                }
            }
        }
    }
    return doc
}