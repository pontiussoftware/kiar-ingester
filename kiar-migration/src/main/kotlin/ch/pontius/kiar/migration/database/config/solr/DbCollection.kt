package ch.pontius.kiar.migration.database.config.solr

import ch.pontius.kiar.api.model.config.solr.CollectionType
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A named Apache Solr collection managed by these KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class DbCollection(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbCollection>() {
        fun migrate() {
            all().asSequence().forEach { dbSolrCollection ->
                SolrCollections.insert {
                    it[solrInstanceId] = SolrConfigs.idByName(dbSolrCollection.solr.name) ?: throw IllegalArgumentException("Could not find Apache Solr config with name '${dbSolrCollection.solr.name}'.")
                    it[name] = dbSolrCollection.name
                    it[displayName] = dbSolrCollection.displayName
                    it[type] = CollectionType.valueOf(dbSolrCollection.type.description)
                    it[selector] = dbSolrCollection.selector
                    it[oai] = dbSolrCollection.oai
                    it[deleteBeforeIngest] = dbSolrCollection.deleteBeforeIngest
                }
            }
        }
    }

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
}