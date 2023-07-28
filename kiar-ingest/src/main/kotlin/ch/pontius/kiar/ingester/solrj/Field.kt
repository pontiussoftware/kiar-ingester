package ch.pontius.kiar.ingester.solrj

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class Field(val solr: String, val required: Boolean) {
    /** Field containing an object's display designation. */
    DISPLAY("_display_", true),

    /** Field containing an object's designation. */
    OBJEKTBEZEICHNUNG("objektbezeichnung", true),

    /** Field containing an object's type. */
    OBJEKTTYP("objecttyp", true),

    /** Field containing title information. */
    TITEL("titel", false),

    /** Field containing author information (person). */
    AUTOR("person_name_autor", false),

    /** Field containing author information (person). */
    KUENSTLER("person_name_kuenstler", false),

    /** Field containing author information (person). */
    FOTOGRAF("person_name_fotograf", false);
}