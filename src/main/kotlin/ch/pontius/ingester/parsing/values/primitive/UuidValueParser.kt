package ch.pontius.ingester.parsing.values.primitive

import ch.pontius.ingester.parsing.values.ValueParser
import java.util.UUID

/**
 * [ValueParser] to convert a [String] to a [UUID].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class UuidValueParser: ValueParser<UUID> {
    private var buffer: UUID? = null
    override fun parse(value: String) {
        this.buffer = UUID.fromString(value)
    }
    override fun get(): UUID? = this.buffer
}