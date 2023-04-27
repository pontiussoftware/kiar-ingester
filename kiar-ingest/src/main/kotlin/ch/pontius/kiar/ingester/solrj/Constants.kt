package ch.pontius.kiar.ingester.solrj

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Constants {

    /** Name of the version field in an Apache Solr document. */
    const val FIELD_NAME_VERSION = "_version_"

    /** Name of the random field in an Apache Solr document. */
    const val FIELD_NAME_RANDOM = "_random_"

    /** Name of the UUID field in an Apache Solr document. */
    const val FIELD_NAME_RAW = "_raw_"

    /** Name of the participant field in an Apache Solr document. */
    const val FIELD_NAME_OUTPUT = "_output_"

    /** Name of the participant field in an Apache Solr document. */
    const val FIELD_NAME_PARTICIPANT = "_participant_"

    /** Name of the canton field in an Apache Solr document. */
    const val FIELD_NAME_CANTON = "_canton_"

    /** Name of the image count field in an Apache Solr document. */
    const val FIELD_NAME_IMAGECOUNT = "_imagecount_"

    /** Name of the display field in an Apache Solr document. */
    const val FIELD_NAME_DISPLAY = "_display_"

    /** Name of the UUID field in an Apache Solr document. */
    const val FIELD_NAME_UUID = "uuid"

    /** Name of the 'inventarnummer' field in an Apache Solr document. */
    const val FIELD_NAME_INVENTORY_NUMBER = "inventarnummer"

    /** Name of the 'objekttyp' field in an Apache Solr document. */
    const val FIELD_NAME_OBJECTTYPE = "objekttpy"

    /** Set of system fields to ignore upon ingest. */
    val SYSTEM_FIELDS = setOf(FIELD_NAME_VERSION, FIELD_NAME_RANDOM)

    /** Set of system fields to ignore upon ingest. */
    val INTERNAL_FIELDS = setOf(FIELD_NAME_RAW, FIELD_NAME_OUTPUT)
}