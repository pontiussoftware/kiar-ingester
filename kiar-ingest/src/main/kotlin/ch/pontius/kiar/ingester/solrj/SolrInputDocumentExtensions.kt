package ch.pontius.kiar.ingester.solrj

import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument


/**
 * Checks if the [SolrInputDocument] has the specified [Field].
 *
 * @return True if [Field] is contained in [SolrInputDocument], false otherwise.
 */
fun SolrDocument.uuid(): String = this.get<String>(Field.UUID) ?: throw IllegalArgumentException("Field 'uuid' is missing. This is a programmer's error, since such entries should be filtered at the source.")

/**
 * Checks if the [SolrInputDocument] has the specified [Field].
 *
 * @return True if [Field] is contained in [SolrInputDocument], false otherwise.
 */
fun SolrInputDocument.uuid(): String = this.get<String>(Field.UUID) ?: throw IllegalArgumentException("Field 'uuid' is missing. This is a programmer's error, since such entries should be filtered at the source.")

/**
 * Checks if the [SolrDocument] has the specified [Field].
 *
 * @return True if [Field] is contained in [SolrInputDocument], false otherwise.
 */
fun SolrDocument.has(field: Field): Boolean = this.containsKey(field.solr)

/**
 * Checks if the [SolrInputDocument] has the specified [Field].
 *
 * @return True if [Field] is contained in [SolrInputDocument], false otherwise.
 */
fun SolrInputDocument.has(field: Field): Boolean = this.containsKey(field.solr)

/**
 * Returns the provided [Field] from this [SolrDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrDocument.get(field: Field): T? = this.getFieldValue(field.solr) as? T

/**
 * Returns the provided [Field] from this [SolrInputDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrInputDocument.get(field: Field): T? = this.getFieldValue(field.solr) as? T

/**
 * Returns the provided [Field] from this [SolrDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrDocument.getAll(field: Field): List<T> = this.getFieldValues(field.solr)?.filterIsInstance<T>() ?: emptyList()

/**
 * Returns the provided [Field] from this [SolrInputDocument].
 *
 * @param field The [Field] to return.
 * @return The [Field]'s value [T] or null
 */
inline fun <reified T> SolrInputDocument.getAll(field: Field): List<T> = this.getFieldValues(field.solr)?.filterIsInstance<T>() ?: emptyList()

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
    require(field.multiValued) { "The field '${field.solr}' is not a multi-valued field. This is a programmer's error." }
    this.addField(field.solr, value)
}