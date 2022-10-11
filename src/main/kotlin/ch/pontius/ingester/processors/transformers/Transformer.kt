package ch.pontius.ingester.processors.transformers

import ch.pontius.ingester.processors.sinks.Sink
import ch.pontius.ingester.processors.sources.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * A [Transformer] acts both as a [Sink] and [Source].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Transformer<I,O>: Sink<I>, Source<O> {
    override fun execute(): Flow<Unit> = flow {
        this@Transformer.toFlow().collect()
    }
}