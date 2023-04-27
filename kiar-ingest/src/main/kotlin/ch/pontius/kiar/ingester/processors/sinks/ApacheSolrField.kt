package ch.pontius.kiar.ingester.processors.sinks

import org.apache.solr.common.SolrInputDocument

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApacheSolrField(val name: String, val required: Boolean, val multiValued: Boolean) {
    /**
     * Checks if [SolrInputDocument] is valid for ingest. Returns true if so, and false otherwise.
     *
     * @param input The [SolrInputDocument] to check.
     * @return True if [SolrInputDocument] is valid, false otherwise.
     */
    fun isValid(input: SolrInputDocument): Boolean {
        val field = input[this.name]
        if (this.required && (field == null || field.valueCount == 0)) return false
        if (field != null) {
            if (!this.multiValued &&  field.valueCount > 1) return false
        }
        return true
    }

    /**
     * Checks if [SolrInputDocument] is valid for ingest. Returns an explanation if not.
     *
     * @param input The [SolrInputDocument] to check.
     * @return Explanation for why the [SolrInputDocument] is invalid.
     */
    fun isInvalidReason(input: SolrInputDocument): String {
        val field = input[this.name]
        if (this.required && (field == null || field.valueCount == 0)) return "Field ${this.name} is required but empty."
        if (field != null) {
            if (!this.multiValued &&  field.valueCount > 1) return "Field ${this.name} is single-valued but contains multiple values."
        }
        throw IllegalStateException("SolrInputDocument seems to be valid.")
    }
}