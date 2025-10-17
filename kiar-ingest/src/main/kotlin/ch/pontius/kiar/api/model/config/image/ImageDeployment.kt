package ch.pontius.kiar.api.model.config.image

import kotlinx.serialization.Serializable

typealias ImageDeploymentId = Int

/**
 * An image deployment configuration.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class ImageDeployment(
    val id: ImageDeploymentId? = null,
    val name: String,
    val format: ImageFormat,
    val source: String,
    val path: String,
    val server: String?,
    val maxSize: Int
)