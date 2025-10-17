package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.solr.ApacheSolrConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.select

/**
 * A [IntIdTable] that holds information about [SolrConfigs].
 *
 * [SolrConfigs] configure an Apache Solr instance for data ingest.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object SolrConfigs: IntIdTable("solr_configs") {
    /** The name of a [SolrConfigs] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The optional description of a [SolrConfigs] entry. */
    val description = text("description").nullable()

    /** URL pointing to the Apache Solr server backing this [SolrConfigs] entry. */
    val server = text("server")

    /** Public URL pointing to the Apache Solr server backing this [SolrConfigs] entry (if different from server). */
    val publicServer = text("server_public").nullable()

    /** Apache Solr user's username that a [SolrConfigs] uses. */
    val username = text("username").nullable()

    /** Apache Solr user's password that a [SolrConfigs] uses. */
    val password = text("password").nullable()

    /** Timestamp of creation of the [SolrConfigs] entry. */
    val created = timestamp("created").defaultExpression(CurrentTimestamp)

    /** Timestamp of change of the [SolrConfigs] entry. */
    val modified = timestamp("modified").defaultExpression(CurrentTimestamp)

    /**
     * Obtains a [SolrConfigs] [id] by its [name]-
     *
     * @param name The name to lookup
     * @return [SolrConfigs] [id] or null, if no entry exists.
     */
    fun idByName(name: String) =  SolrConfigs.select(id).where { SolrConfigs.name eq name }.map { it[id].value }.firstOrNull()

    /**
     * Converts this [ResultRow] into an [ApacheSolrConfig].
     *
     * @return [ApacheSolrConfig]
     */
    fun ResultRow.toSolr() = ApacheSolrConfig(
        id = this[id].value,
        name = this[name],
        description = this[description],
        server = this[server],
        publicServer = this[publicServer],
        username = this[username],
        password = this[password],
        createdAt = this[created].toEpochMilli(),
        changedAt = this[modified].toEpochMilli()
    )
}