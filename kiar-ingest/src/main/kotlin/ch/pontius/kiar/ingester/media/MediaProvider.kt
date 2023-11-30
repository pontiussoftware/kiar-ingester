package ch.pontius.kiar.ingester.media

import com.sksamuel.scrimage.ImmutableImage

/**
 * A proxy for a media resource of type [T]. Used for memory-efficient processing.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
sealed interface MediaProvider<T> {

    /**
     * Opens the media backing this [MediaProvider].
     *
     * @return [T] or null
     */
    fun open(): T?

    /**
     * A [MediaProvider] for [ImmutableImage]s.
     */
    interface Image: MediaProvider<ImmutableImage>
}