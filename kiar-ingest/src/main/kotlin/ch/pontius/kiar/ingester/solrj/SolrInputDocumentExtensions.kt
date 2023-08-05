package ch.pontius.kiar.ingester.solrj

import org.apache.solr.common.SolrInputDocument


/**
 *
 */
fun SolrInputDocument.has(field: Field): Boolean = this.containsKey(field.solr)

/**
 * Returns the provided [Field] from this [SolrInputDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrInputDocument.get(field: Field): T? = this.getFieldValue(field.name) as? T

/**
 * Returns the provided [Field] from this [SolrInputDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrInputDocument.getAll(field: Field): Collection<T> {
    require(field.multiValued) { "The field '${field.name}' is not a multi-valued field. This is a programmer's error." }
    return this.getFieldValues(field.name).filterIsInstance<T>()
}

/**
 * Returns a [Field]'s value in this [SolrInputDocument] as [String].
 *
 * For multi-valued fields, only the first value is returned.
 *
 * @param field The [Field] to retrieve.
 * @return The value as [String] or null.
 */
fun SolrInputDocument.asString(field: Field) : String? = this[field.solr]?.firstValue as? String

/**
 * Sets a [Field]'s value in this [SolrInputDocument].
 *
 * @param field The [Field] to populate.
 * @param value The value.
 */
fun SolrInputDocument.setField(field: Field, value: Any) = this.setField(field.solr, value)

/**
 * Adds a [Field]'s value in this [SolrInputDocument].
 *
 * Checks if the corresponding [Field] is [Field.multiValued]. Otherwise, an [IllegalArgumentException] is thrown.
 *
 * @param field The [Field] to populate.
 * @param value The value.
 */
fun SolrInputDocument.addField(field: Field, value: Any) {
    require(field.multiValued) { "The field '${field.name}' is not a multi-valued field. This is a programmer's error." }
    this.addField(field.solr, value)
}

/**
 * Removes a [Field] from this [SolrInputDocument].
 *
 * @param field The [Field] to remove.
 */
fun SolrInputDocument.removeField(field: Field): Boolean = this.removeField(field.name) != null