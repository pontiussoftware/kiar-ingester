package ch.pontius.kiar.ingester.config

import ch.pontius.kiar.ingester.solrj.Constants
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class CollectionConfig (
    /** The name of the Apache Solr collection. */
    val name: String,

    /** A list of keywords that maps incoming objects to a [SolrConfig] based on the content of the _output_ field. */
    val filter: List<String> = emptyList(),

    /** Flag indicating, that  collection should be purged before starting an import. */
    val deleteBeforeImport: Boolean = true,

    /** A list of keywords that maps incoming objects to a [SolrConfig] based on the content of the _output_ field. */
    val acceptEmptyFilter: Boolean = false
) {

    /**
     * Checks if the provided [SolrInputDocument] matches this [CollectionConfig] based on the _output_ field.
     *
     * @return True if [SolrInputDocument] is a match, false otherwise.
     */
    fun isMatch(doc: SolrInputDocument): Boolean {
        if (this.acceptEmptyFilter && doc.getFieldValues(Constants.FIELD_NAME_OUTPUT).isEmpty()) return true
        return doc.getFieldValues(Constants.FIELD_NAME_OUTPUT).contains(this.filter)
    }
}