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

    /** Field containing date of the last change. */
    LASTCHANGE("_lastchange_", true, false, false),

    /** Field used to store the number of available images. */
    IMAGECOUNT("_imagecount_", true, false, false),

    /** Field used to store the number of available images. */
    PREVIEW("_previews_", false, true, false),

    /** Field used to store the number of available images. */
    IMAGE_ARTISTS("_images_metadata_artist_", false, true, false),

    /** Field used to store the number of available images. */
    IMAGE_COPYRIGHT("_images_metadata_copyright_", false, true, false),

    /** Name of the participant an object belongs to (often, this is transitively dependent on the institution). */
    PARTICIPANT("_participant_", true),

    /** Name of the canton field an object belongs to (transitively dependent on the institution). */
    CANTON("_canton_", true),

    /** Field containing an object's display designation. */
    DISPLAY("_display_", true),

    /** Field containing an object's display designation. */
    DISPLAY_LIST("_display_list_", true, true),

    /** Field containing the object's UUID. */
    UUID("uuid", true),

    /** Field containing the object's inventory number. */
    INVENTORY_NUMBER("inventarnummer", true),

    /** Field containing the object's ISBN number. */
    ISBN("isbn", false),

    /** Field containing an object's designation. */
    DESIGNATION("objektbezeichnung", true),

    /** Field containing an object's designation. */
    ALTERNATIVE_DESIGNATION("alternative_objektbezeichnung", false),

    /** Field containing an object's description. */
    DESCRIPTION("description", false),

    /** Field containing an object's type. */
    OBJECTTYPE("objekttyp", true),

    /** Field containing the name of the institution the object belongs to. */
    COLLECTION("sammlung", true),

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

    /** The language of an object. */
    LANGUAGE("sprache", false),

    /* Material & technique. */

    /** */
    MATERIAL("material", false),

    /** */
    TECHNIQUE("material", false),


    /* Classifications. */

    ICONOGRAPHY("klassifikation_ikonographie", false),

    SUBJECT("klassifikation_ikonographie", false),

    TYPOLOGY("klassifikation_typologie", false),


    /* Persons */

    /** Field containing publisher information (person). */
    PUBLISHER("person_name_verlag", false),

    /** Field containing author information (person). */
    AUTHOR("person_name_autor", false),

    /** Field containing artist information (person). */
    ARTIST("person_name_kuenstler", false),

    /** Field containing photographer information (person). */
    PHOTOGRAPHER("person_name_fotograf", false),

    /** Field containing producer information (person). */
    CREATOR("person_name_hersteller", false),


    /* Orte */

    /** Field containing location of manufacture information . */
    HERSTELLUNGSORT("ort_herstellung", false),

    /** Field containing location of finding information. */
    POLITISCHER_FUNDORT("ort_fund", false),

    /** Field containing location of publication information. */
    ERSCHEINUNGSORT("ort_erscheinung", false);
}