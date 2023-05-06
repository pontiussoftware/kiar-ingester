package ch.pontius.kiar.utilities

import kotlinx.dnq.XdEntity
import kotlinx.dnq.query.XdQuery
import kotlinx.dnq.query.asIterable
import kotlinx.dnq.query.size


/**
 * A convenience method that maps the results of [XdQuery] into an [Array].
 *
 * @return [Array] of type [R]
 */
inline fun <T: XdEntity, reified R> XdQuery<T>.mapToArray(mapping: (T) -> R): Array<R> {
    val size = this.size()
    val iterable = this.asIterable().iterator()
    return Array(size) { mapping(iterable.next()) }
}