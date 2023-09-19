package ch.pontius.kiar.api.model.config.image

import ch.pontius.kiar.database.config.image.DbImageFormat
import kotlinx.serialization.Serializable

/**
 * The types of [ImageFormat]s supported by KIAR tools as deployment format.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
enum class ImageFormat {
    JPEG, PNG;

    /**
     * Convenience method to convert this [ImageFormat] to a [DbImageFormat]. Requires an ongoing transaction!
     *
     * @return [DbImageFormat]
     */
    fun toDb() = when(this) {
        JPEG -> DbImageFormat.JPEG
        PNG ->  DbImageFormat.PNG
    }
}