package ch.pontius.kiar.database.institutions

import ch.pontius.kiar.database.config.SolrCollections
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * A mapping [Table] that can be used to configure [SolrCollections] per [Institutions].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object InstitutionsSolrCollections : Table("institutions_solr_collections") {
    /** Reference to the [Institutions] entry a [InstitutionsSolrCollections] belongs. */
    val institutionId = reference("institution_id", Institutions,  onDelete = ReferenceOption.CASCADE)

    /** Reference to the [SolrCollections] entry a [InstitutionsSolrCollections] configures. */
    val solrCollectionId = reference("solr_collection_id", SolrCollections,  onDelete = ReferenceOption.CASCADE)

    /** Flag indicating that the [SolrCollections] entry is available for an [Institutions] entry. */
    val available = bool("available").default(true)

    /** Flag indicating that the [SolrCollections] entry has been activated for an [Institutions] entry. */
    val selected = bool("selected").default(false)

    /** Timestamp of creation of the [InstitutionsSolrCollections] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [InstitutionsSolrCollections] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /** The [Table.PrimaryKey] is determined by the [institutionId] and the [solrCollectionId]. */
    override val primaryKey = PrimaryKey(this.institutionId, this.solrCollectionId)
}