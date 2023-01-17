package ch.pontius.ingester.processors.sources

import kotlinx.coroutines.flow.Flow

/**
 * A [Source] in a processing pipeline. Generates an output of type [T].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Source<O> {
    /** Name of this [Source]'s context. */
    val context: String

    /**
     * Transforms this [Source] to a flow and returns that flow.
     *
     * @return [Flow] of this [Source].
     */
    fun toFlow(): Flow<O>
}