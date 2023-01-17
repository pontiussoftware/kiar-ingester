package ch.pontius.ingester.processors.transformers

import ch.pontius.ingester.processors.sinks.Sink
import ch.pontius.ingester.processors.sources.Source

/**
 * A [Transformer] acts both as a [Sink] and [Source].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Transformer<I,O>: Source<O> {
    /** The [Source] that acts as input to this [Transformer]. */
    val input: Source<I>

    /** The [context]name  of this [Transformer] is inherited from the [input]. */
    override val context: String
        get() = this.input.context
}