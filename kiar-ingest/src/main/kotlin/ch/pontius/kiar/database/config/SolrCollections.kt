package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.api.model.config.solr.CollectionType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select


/**
 * A [IntIdTable] that holds information about [SolrCollections].
 *
 * [SolrCollections] configure an Apache Solr collection for data ingest
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object SolrCollections: IntIdTable("solr_collections") {
    /** Reference to the [SolrConfigs] entry a [SolrCollections] belongs to. */
    val solrInstanceId = reference("solr_instance_id", SolrConfigs, ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)

    /** The name of the [SolrCollections] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The optional name of the [SolrCollections] entry. */
    val displayName = varchar("display_name", 255).nullable()

    /** The [SolrCollections] of a [SolrCollections] entry. */
    val type = enumerationByName("type", 16, CollectionType::class)

    /** The optional selector for the [SolrCollections] entry. */
    val selector = varchar("selector", 255).nullable()

    /** Flag indicating, that the [SolrCollections] entry should be exposed via OAI endpoint. */
    val oai = bool("oai").default(false)

    /** Flag indicating, that the [SolrCollections] entry should be exposed via SRU endpoint. */
    val sru = bool("sru").default(false)

    /** Flag indicating, that the [SolrCollections] entry should be deleted before ingest starts. */
    val deleteBeforeIngest = bool("delete_before_ingest").default(false)

    /**
     * Obtains a [SolrCollections] [id] by its [name],
     *
     * @param name The name to lookup
     * @return [SolrCollections] [id] or null, if no entry exists.
     */
    fun idByName(name: String) = SolrCollections.select(id).where { SolrCollections.name eq name}.map { it[id] }.firstOrNull()

    /**
     * Converts this [ResultRow] into an [ApacheSolrCollection].
     *
     * @return [ApacheSolrCollection]
     */
    fun ResultRow.toSolrCollection() = ApacheSolrCollection(
        id = this[id].value,
        name = this[name],
        displayName = this[displayName],
        type = this[type],
        selector = this[selector],
        oai = this[oai],
        sru = this[sru],
        deleteBeforeIngest = this[deleteBeforeIngest]
    )
}