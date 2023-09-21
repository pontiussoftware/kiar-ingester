package ch.pontius.kiar.ingester.processors.sinks

import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.SolrInputField

/**
 * A interface used for (clint-side) validation during inest.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
sealed interface FieldValidator {
    /** Name of the [FieldValidator]. */
    val name: String

    /** Flag indicating, that [FieldValidator] is required. */
    val required: Boolean

    /** Flag indicating, that [FieldValidator] is multi-valued. */
    val multiValued: Boolean

    /** Flag indicating, that [FieldValidator] has a default value. */
    val hasDefault: Boolean

    /**
     * Compares the provided [SolrInputField] name to this [name] and returns true,if there is a match
     * (i.e., this [FieldValidator] corresponds to the given name).
     *
     * @param name The name of the field to match.
     * @return True, if name is an exact match.
     */
    fun isMatch(name: String): Boolean

    /**
     * Checks if [SolrInputField] is valid for ingest given this [FieldValidator]. Returns true if so, and false otherwise.
     *
     * @param field The [SolrInputField] to check.
     * @return True if [SolrInputDocument] is valid, false otherwise.
     */
    fun isValid(field: SolrInputField): Boolean {
        require(field.name == this.name) { "Provided field '${field.name}' does not match field '${this.name}'. This is a programmer's error!" }
        if (this.required && !this.hasDefault && (field.valueCount == 0)) return false
        /* TODO: Type-based validation. */
        return !(!this.multiValued && field.valueCount > 1)
    }

    /**
     * Checks if [SolrInputField] is valid for ingest. Returns an explanation if not.
     *
     * @param field The [SolrInputField] to check.
     * @return Explanation for why the [SolrInputField] is invalid, or null if it's okay.
     */
    fun getReason(field: SolrInputField): String? {
        require(field.name == this.name) { "Provided field '${field.name}' does not match field '${this.name}'. This is a programmer's error!" }
        if (this.required && !this.hasDefault && (field.valueCount == 0)) return "Field ${this.name} is required but does not contain any values."
        /* TODO: Type-based validation. */
        if (!this.multiValued && field.valueCount > 1) return "Field ${this.name} is single-valued but contains multiple values."
        return null
    }

    /**
     * A [Fixed] field with a definde name.
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    data class Fixed(override val name: String, override val required: Boolean, override val multiValued: Boolean, override val hasDefault: Boolean): FieldValidator {

        /**
         * Compares the provided [SolrInputField] name to this [name] and returns true,if there is a match
         * (i.e., this [FieldValidator] corresponds to the given name).
         *
         * For [FieldValidator.Fixed] we expect an exact match.
         *
         * @param name The name of the field to match.
         * @return True, if name is an exact match.
         */
        override fun isMatch(name: String): Boolean = this.name == name
    }

    /**
     * A [Dynamic] field with a wildcard name.
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    data class Dynamic(override val name: String, override val required: Boolean, override val multiValued: Boolean, override val hasDefault: Boolean): FieldValidator {

        /** [Regex] used */
        private val regex by lazy { this.name.replace("*", ".*").toRegex() }

        /**
         * Compares the provided [SolrInputField] name to this [name] and returns true, if there is a match
         * (i.e., this [FieldValidator] corresponds to the given name).
         *
         * For [FieldValidator.Dynamic] we expect a wildcard match.
         *
         * @param name The name of the field to match.
         * @return True, if name is an exact match.
         */
        override fun isMatch(name: String): Boolean = this.regex.matches(name)
    }
}