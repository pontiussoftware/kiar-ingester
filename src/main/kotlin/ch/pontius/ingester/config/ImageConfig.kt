package ch.pontius.ingester.config

import ch.pontius.ingester.serializers.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@kotlinx.serialization.Serializable
data class ImageConfig(
    val name: String,
    val maxSize: Int = 1280,

    @Serializable(with = PathSerializer::class)
    val deployTo: Path,

    @Serializable(with = PathSerializer::class)
    val watermark: Path? = null
)