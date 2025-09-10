package ch.pontius.kiar.ingester.solrj

/**
 * The [ObjectTypes] supported by KIM.ch Data Ingest platform.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
enum class ObjectType(val designation: String) {
    ARCHAEOLOGIE("Archäologie"),
    AUDIOVISUELLES_OBJEKT("Audiovisuelles Objekt"),
    BIBLIOGRAPHISCHES_OBJEKT("Bibliographisches Objekt"),
    BIOLOGIE("Biologie"),
    ETHNOLOGIE("Ethnologie"),
    FOTOGRAFIE("Fotografie"),
    GEOLOGIE("Geologie"),
    KUNST("Kunst");


    companion object {
        /**
         * Tries to parse a [String] into an [ObjectType].
         *
         * @return [ObjectType]
         */
        fun parse(string: String): ObjectType? {
            val u = string.uppercase()
            return ObjectType.entries.find { it.designation.uppercase() == u }
        }
    }
}