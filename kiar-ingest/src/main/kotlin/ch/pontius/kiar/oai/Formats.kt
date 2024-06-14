package ch.pontius.kiar.oai

/**
 * Formats supported by the [OaiServer].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Formats(val prefix: String, val schema: String, val namespace: String) {
    EDM("edm", "https://www.europeana.eu/schemas/edm/EDM.xsd", "http://www.europeana.eu/schemas/edm/"),
}