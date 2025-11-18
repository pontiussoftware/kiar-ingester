package ch.pontius.kiar.servers.mapper

/**
 * Formats supported by the [ch.pontius.kiar.servers.oai.OaiServer].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Formats(val prefix: String, val schema: String? = null, val namespace: String? = null) {
    OAI_DC("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "http://www.openarchives.org/OAI/2.0/oai_dc/"),
    EDM("edm", "https://www.europeana.eu/schemas/edm/EDM.xsd", "http://www.europeana.eu/schemas/edm/"),
    SOLR("solr", null, null);

    /**
     * Generates a new [Mapper] for this [Formats].
     *
     * @return [Mapper]
     */
    fun toMapper(): Mapper = when (this) {
        OAI_DC -> DCMapper
        SOLR -> SolrMapper
        EDM -> EDMMapper()
    }
}