package ch.pontius.kiar.oai

import ch.pontius.kiar.oai.mapper.OAIDCMapper
import ch.pontius.kiar.oai.mapper.EDMMapper
import ch.pontius.kiar.oai.mapper.OAIMapper
import jetbrains.exodus.database.TransientEntityStore

/**
 * Formats supported by the [OaiServer].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Formats(val prefix: String, val schema: String, val namespace: String) {
    OAI_DC("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/"),
    EDM("edm", "https://www.europeana.eu/schemas/edm/EDM.xsd", "http://www.europeana.eu/schemas/edm/");

    /**
     * Generates a new [OAIMapper] for this [Formats].
     *
     * @param store [TransientEntityStore] to use.
     * @return [OAIMapper]
     */
    fun toMapper(store: TransientEntityStore): OAIMapper = when (this) {
        OAI_DC -> OAIDCMapper
        EDM -> EDMMapper(store)
    }
}