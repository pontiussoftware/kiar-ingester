package ch.pontius.kiar.ingester.solrj

import org.apache.solr.common.SolrInputDocument


/**
 *
 */
fun SolrInputDocument.has(field: Field): Boolean = this.containsKey(field.solr)

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