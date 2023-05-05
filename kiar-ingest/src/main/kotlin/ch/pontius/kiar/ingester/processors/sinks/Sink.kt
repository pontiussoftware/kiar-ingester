package ch.pontius.kiar.ingester.processors.sinks

import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.transformers.Transformer
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

    /** The [context]name  of this [Sink] is inherited from the [input]. */
    val context: String
        get() = this.input.context

    /**
     * Transforms this [Sink] to a flow and returns that flow.
     *
     * @return [Flow] of this [Sink].
     */
    fun toFlow(): Flow<Unit>
}