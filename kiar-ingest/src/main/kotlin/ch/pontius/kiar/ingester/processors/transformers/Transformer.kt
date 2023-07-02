package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.ingester.processors.sinks.Sink
import ch.pontius.kiar.ingester.processors.sources.Source

/**
 * A [Transformer] acts both as a [Sink] and [Source].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Transformer<I,O>: Source<O> {
    /** The [Source] that acts as input to this [Transformer]. */
    val input: Source<I>
}