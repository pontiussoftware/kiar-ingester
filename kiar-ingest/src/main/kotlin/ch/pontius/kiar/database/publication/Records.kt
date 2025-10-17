package ch.pontius.kiar.database.publication

import ch.pontius.kiar.database.publication.Records.uuid
import com.google.gson.Gson
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.SolrInputField
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

/**
 * A [Table] that holds information about published [Records].
 *
 * [Records] entries represent published object records. The table is basically a
 * staging are for such [Records]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Records: Table("records") {
    /** [Gson] instance. */
    val gson = Gson()

    /** The unique identifier of a [Records] entry. */
    val uuid = uuid("uuid")

    /** The unique identifier of a [Records] entry. */
    val collection = varchar("collection", 64)

    /** The local number of a [Records] entry. */
    val local_number = varchar("local_number", 255)

    /** The designation of a [Records] entry. */
    val designation = varchar("designation", 255)

    /** The designation of a [Records] entry. */
    val institution = varchar("institution", 255)

    /** The designation of a [Records] entry. */
    val document = json<SolrInputDocument>("document", { it -> gson.toJson(it.toMap()) }, { SolrInputDocument(gson.fromJson(it, Map::class.java) as Map<String, SolrInputField>) })

    /** The [uuid] field acts as primary key. */
    override val primaryKey = PrimaryKey(this.uuid, this.collection)
}
