package ch.pontius.kiar.api.model.config.image

import kotlinx.serialization.Serializable

/**
 * An image deployment configuration.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ImageDeployment(val name: String, val format: ImageFormat, val path: String, val server: String, val maxSize: Int)