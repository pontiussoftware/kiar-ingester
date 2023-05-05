package ch.pontius.kiar.database.config.solr

import ch.pontius.kiar.config.CollectionConfig
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A named Apache Solr collection managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbCollection(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbCollection>()

    /** The name held by this [DbCollection]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The [DbCollectionType] of this [DbCollection]*/
    var type by xdLink1(DbCollectionType)

    /** The collection filters employed by this [DbCollection]. */
    var filters by xdStringProp(trimmed = true)

    /** Flag indicating, that the [DbCollection] should be deleted before starting an ingest. */
    var deleteBeforeIngest by xdBooleanProp()

    /** A flag indicating, that this [DbCollection] accepts empty filters. */
    var acceptEmptyFilter by xdBooleanProp()

    /** [DbSolr] instance this [DbCollection] belongs to. */
    var solr: DbSolr by xdParent(DbSolr::collections)

    /**
     * A convenience method used to convert this [DbCollection] to a [CollectionConfig]. Requires an ongoing transaction!
     *
     * @return [CollectionConfig]
     */
    fun toApi() = CollectionConfig(this.name, emptyList(), this.deleteBeforeIngest, this.acceptEmptyFilter)
}