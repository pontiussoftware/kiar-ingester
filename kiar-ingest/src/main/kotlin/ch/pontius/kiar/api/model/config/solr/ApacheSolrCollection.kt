package ch.pontius.kiar.api.model.config.solr

import ch.pontius.kiar.config.CollectionConfig
import ch.pontius.kiar.ingester.solrj.Constants
import kotlinx.serialization.Serializable
import org.apache.solr.common.SolrInputDocument

/**
 * Configuration regarding an Apache Solr collection.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ApacheSolrCollection(
    /** The name of the Apache Solr collection. */
    val name: String,

    /** The type of [ApacheSolrCollection]. */
    val type: CollectionType,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val selector: String?,

    /** Flag indicating, that  collection should be purged before starting an import. */
    val deleteBeforeImport: Boolean = true,

    /** A list of keywords that maps incoming objects to a [ApacheSolrConfig] based on the content of the _output_ field. */
    val acceptEmptyFilter: Boolean = false
) {
    /**
     * Checks if the provided [SolrInputDocument] matches this [CollectionConfig] based on the _output_ field.
     *
     * @return True if [SolrInputDocument] is a match, false otherwise.
     */
    fun isMatch(doc: SolrInputDocument): Boolean {
        if (this.selector == null) return true
        val field = doc.getFieldValues(Constants.FIELD_NAME_OUTPUT) ?: return false
        return field.contains(this.selector)
    }
}