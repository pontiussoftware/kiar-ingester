package ch.pontius.kiar.database

import ch.pontius.kiar.database.config.AttributeMappings
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.Transformers
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.jobs.JobLogs
import ch.pontius.kiar.database.jobs.Jobs
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * The database [Schema] used by Data Ingest Portal (KIAR).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Schema {

    /** [List] of [Table]s known to this [Schema]. */
    private val tables = arrayOf<Table>(
        AttributeMappings,
        EntityMappings,
        ImageDeployments,
        Institutions,
        InstitutionsSolrCollections,
        Jobs,
        JobLogs,
        Participants,
        SolrCollections,
        SolrConfigs,
        JobTemplates,
        Transformers,
        Users
    )

    /**
     * Initializes this [Schema] (if necessary).
     */
    fun initialize(database: Database)= transaction {
        SchemaUtils.create(*this@Schema.tables)
    }

    /**
     * Performs a check on the database schema and makes sure that all tables are present.
     *
     * @param database The [Database] to check the schema on.
     * @return True, if all database tables are present. False otherwise.
     */
    fun check(database: Database): Boolean = transaction {
        val available = SchemaUtils.listTables().map {
            it.split(".").last()
        }
        this@Schema.tables.all { t1 ->
            available.any { t2 ->
                t1.nameInDatabaseCase() == t2
            }
        }
    }
}