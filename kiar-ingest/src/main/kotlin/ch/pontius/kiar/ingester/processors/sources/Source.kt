package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.ingester.processors.ProcessingContext
import kotlinx.coroutines.flow.Flow

/**
 * A [Source] in a processing pipeline. Generates an output of type [T].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Source<O> {
    /**
     * Transforms this [Source] to a flow and returns that flow.
     *
     * @param context The [ProcessingContext] for this [Source].
     * @return [Flow] of this [Source].
     */
    fun toFlow(context: ProcessingContext): Flow<O>
}