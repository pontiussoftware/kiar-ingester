package ch.pontius.ingester.processors.sinks

import ch.pontius.ingester.processors.sources.Source
import kotlinx.coroutines.flow.Flow


/**
 * A [Sink] builds the endpoint of a processing pipeline.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Sink<I> {
    /** The [Source] that acts as input to this [Sink]. */
    val input: Source<I>

    /**
     * Transforms this [Sink] to a flow and returns that flow.
     *
     * @return [Flow] of this [Sink].
     */
    fun execute(): Flow<Unit>
}