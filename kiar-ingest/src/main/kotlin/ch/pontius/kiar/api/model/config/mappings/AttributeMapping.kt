package ch.pontius.kiar.api.model.config.mappings

import ch.pontius.kiar.ingester.parsing.values.images.FileImageValueParser
import ch.pontius.kiar.ingester.parsing.values.images.MuseumplusImageParser
import ch.pontius.kiar.ingester.parsing.values.images.URLImageValueParser
import ch.pontius.kiar.ingester.parsing.values.primitive.*
import ch.pontius.kiar.ingester.parsing.values.struct.LV95Parser
import ch.pontius.kiar.ingester.parsing.values.struct.WGS84Parser
import kotlinx.serialization.Serializable

/**
 * A [AttributeMapping] definition defines how an individual XML attribute should be mapped to a destination attribute.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class AttributeMapping(
    val source: String,
    val destination: String,
    val parser: ValueParser,
    val required: Boolean = false,
    val multiValued: Boolean = false,
    val parameters: Map<String,String> = emptyMap(),
) {

    /**
     * Returns a new [ValueParser] instance for this [AttributeMapping] value.
     *
     * @return [ValueParser].
     */
    fun newParser() = when (this.parser) {
        ValueParser.UUID -> UuidValueParser(this)
        ValueParser.STRING -> StringValueParser(this)
        ValueParser.MULTISTRING -> MultiStringValueParser(this)
        ValueParser.INTEGER -> IntegerValueParser(this)
        ValueParser.DOUBLE -> DoubleValueParser(this)
        ValueParser.DATE -> DateValueParser(this)
        ValueParser.COORD_WGS84 -> WGS84Parser(this)
        ValueParser.COORD_LV95 -> LV95Parser(this)
        ValueParser.IMAGE_FILE -> FileImageValueParser(this)
        ValueParser.IMAGE_URL -> URLImageValueParser(this)
        ValueParser.IMAGE_MPLUS -> MuseumplusImageParser(this)
    }
}