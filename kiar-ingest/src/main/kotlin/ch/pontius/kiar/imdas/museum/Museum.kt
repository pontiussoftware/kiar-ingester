package ch.pontius.kiar.imdas.museum

import java.sql.ResultSet

/**
 * A [Museum] as loaded by the [MuseumAccessor].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class Museum(val name: String, val isil: String, val street: String?, val city: String?, val zip: Int?, val canton: String, val email: String?, val website: String?) {
    constructor(resultSet: ResultSet) : this(
        resultSet.getString("name"),
        resultSet.getString("isil"),
        resultSet.getString("street"),
        resultSet.getString("city"),
        resultSet.getString("postcode")?.toIntOrNull(),
        resultSet.getString("canton"),
        resultSet.getString("email"),
        resultSet.getString("website"),
    )
}