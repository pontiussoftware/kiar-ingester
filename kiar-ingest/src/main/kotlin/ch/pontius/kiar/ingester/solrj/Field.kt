package ch.pontius.kiar.ingester.solrj

/**
 * Enumeration of all fields known to the KIM.ch Data Ingest Platform.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Field(val solr: String, val required: Boolean = false, val multiValued: Boolean = false, val transient: Boolean = false) {
    /** A transient field used to store raw image information. */
    RAW("_raw_", false, true, true),

    /** Field used to store the number of available images. */
    IMAGECOUNT("_imagecount_", true, false, false),

    /** Name of the participant an object belongs to (often, this is transitively dependent on the institution). */
    PARTICIPANT("_participant_", true),

    /** Name of the canton field an object belongs to (transitively dependent on the institution). */
    CANTON("_canton_", true),

    /** Field containing an object's display designation. */
    DISPLAY("_display_", true),

    /** Field containing the object's UUID. */
    UUID("uuid", true),

    /** Field containing an object's designation. */
    OBJEKTBEZEICHNUNG("objektbezeichnung", true),

    /** Field containing an object's type. */
    OBJEKTTYP("objecttyp", true),

    /** Field containing the name of the institution the object belongs to. */
    INSTITUTION("institution", true),

    /** Field containing title information. */
    TITEL("titel", false),

    /* Copyright information. */
    /** The rights statement entry used for an object. */
    RIGHTS_STATEMENT("_rights_", true),

    /** The rights statement entry used for an object (long description). */
    RIGHTS_STATEMENT_LONG("_rights_long_", true),

    /** The rights statement entry used for an object (URL). */
    RIGHTS_STATEMENT_URL("_rights_url_", true),

    /** Additional copyright information, in case object is copyrighted. */
    COPYRIGHT("copyright", false),

    /** Optional creditline, in case object one has been assigned to the object. */
    CREDITLINE("creditline", false),

    /** Name of an object's owner. */
    OWNER("eigentuemer", false),

    /* Personen */

    /** Field containing author information (person). */
    AUTOR("person_name_autor", false),

    /** Field containing author information (person). */
    KUENSTLER("person_name_kuenstler", false),

    /** Field containing author information (person). */
    FOTOGRAF("person_name_fotograf", false);
}