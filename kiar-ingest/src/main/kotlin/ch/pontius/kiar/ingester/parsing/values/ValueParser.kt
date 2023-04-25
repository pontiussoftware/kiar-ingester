package ch.pontius.kiar.ingester.parsing.values


/**
 * Interface implemented by a [ValueParser] used to convert a [String] value to type [T]. Used for XML parsing.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface ValueParser<T> {
    /**
     * Parses the given [String] and updates this [ValueParser]'s state.
     *
     * @param value The [String] value to parse.
     */
    fun parse(value: String)

    /**
     * Gets and returns the current value for this [ValueParser]
     */
    fun get(): T?
}