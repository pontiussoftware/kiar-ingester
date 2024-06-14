package ch.pontius.kiar.oai

import ch.pontius.kiar.oai.mapper.EDMMapper
import ch.pontius.kiar.oai.mapper.OAIMapper

/**
 * Formats supported by the [OaiServer].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Formats(val prefix: String, val schema: String, val namespace: String, val mapper: OAIMapper) {
    EDM("edm", "https://www.europeana.eu/schemas/edm/EDM.xsd", "http://www.europeana.eu/schemas/edm/", EDMMapper),
}