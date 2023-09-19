package ch.pontius.kiar.api.model.config.image

import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * An image deployment configuration.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ImageDeployment(val id: String, val name: String, val format: ImageFormat, val deployTo: String, val host: String, val maxSize: Int)