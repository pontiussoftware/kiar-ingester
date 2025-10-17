package ch.pontius.kiar.api.model.config.image

import kotlinx.serialization.Serializable

/**
 * The types of [ImageFormat]s supported by KIAR tools as deployment format.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
enum class ImageFormat {
    JPEG, PNG;
}