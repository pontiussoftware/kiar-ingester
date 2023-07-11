package ch.pontius.kiar.cli.models

import kotlinx.serialization.Serializable

/**
 * A [Institution] as imported from an imdas pro JSON export.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Institution(
    val name: String,
    val isil: String? = null,
    val street: String? = null,
    val postcode: Int,
    val city: String,
    val canton: String,
    val email: String,
    val website: String? = null
)