package ch.pontius.kiar.api.model.config.mappings

import kotlinx.serialization.Serializable

/**
 * The types of [EntityMapping]s supported by KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
enum class MappingFormat {
    XML,   /** XML file based mapping. */
    JSON,  /** JSON file based mapping. */
    EXCEL; /** EXCEL file based mapping. */
}