package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import kotlinx.coroutines.flow.Flow

/**
 * A [Sink] builds the endpoint of a processing pipeline.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface Sink<I> {
    /** The [Source] that acts as input to this [Sink]. */
    val input: Source<I>

    /**
     * Transforms this [Sink] to a flow and returns that flow.
     *
     * @param context The [ProcessingContext] used by this [Sink].
     * @return [Flow] of this [Sink].
     */
    fun toFlow(context: ProcessingContext): Flow<Unit>
}