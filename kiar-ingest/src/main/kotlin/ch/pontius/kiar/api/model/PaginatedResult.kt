package ch.pontius.kiar.api.model

/**
 * A [PaginatedResult] object.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface PaginatedResult<T> {
    val total: Long
    val page: Int
    val pageSize: Int
    val results: List<T>
}
