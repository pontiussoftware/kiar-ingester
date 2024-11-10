package ch.pontius.kiar.database.config.solr

import ch.pontius.kiar.api.model.config.solr.ApacheSolrCollection
import ch.pontius.kiar.config.CollectionConfig
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A named Apache Solr collection managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class DbCollection(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbCollection>()

    /** The name held by this [DbCollection].*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The display name held by this [DbCollection].*/
    var displayName by xdStringProp(trimmed = true)

    /** The [DbCollectionType] of this [DbCollection]*/
    var type by xdLink1(DbCollectionType)

    /** The collection selector for this [DbCollection]. */
    var selector by xdStringProp(trimmed = true)

    /** Flag indicating that the [DbCollection] can be harvested via OAI-PMH. */
    var oai by xdBooleanProp()

    /** Flag indicating, that the [DbCollection] should be deleted before starting an ingest. */
    var deleteBeforeIngest by xdBooleanProp()

    /** [DbSolr] instance this [DbCollection] belongs to. */
    var solr: DbSolr by xdParent(DbSolr::collections)

    /**
     * A convenience method used to convert this [DbCollection] to a [CollectionConfig]. Requires an ongoing transaction!
     *
     * @return [CollectionConfig]
     */
    fun toApi() = ApacheSolrCollection(
        name = this.name,
        displayName = this.displayName,
        type = this.type.toApi(),
        selector = this.selector,
        oai = this.oai,
        deleteBeforeIngest = this.deleteBeforeIngest
    )
}