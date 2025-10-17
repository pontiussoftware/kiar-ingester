package ch.pontius.kiar.migration

import ch.pontius.kiar.database.Schema
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.migration.database.collection.DbObjectCollection
import ch.pontius.kiar.migration.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.migration.database.config.jobs.DbJobType
import ch.pontius.kiar.migration.database.config.mapping.*
import ch.pontius.kiar.migration.database.config.solr.*
import ch.pontius.kiar.migration.database.config.transformers.DbTransformer
import ch.pontius.kiar.migration.database.config.transformers.DbTransformerParameter
import ch.pontius.kiar.migration.database.config.transformers.DbTransformerType
import ch.pontius.kiar.migration.database.institution.DbInstitution
import ch.pontius.kiar.migration.database.institution.DbParticipant
import ch.pontius.kiar.migration.database.institution.DbRole
import ch.pontius.kiar.migration.database.institution.DbUser
import ch.pontius.kiar.migration.database.job.*
import ch.pontius.kiar.migration.database.masterdata.DbCanton
import ch.pontius.kiar.migration.database.masterdata.DbRightStatement
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.query.any
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    /* Sanity check; is path available. */
    if (args.size != 2) {
        System.err.println("Program expects two arguments: Path to source DB and path to destination DB.")
        exitProcess(1)
    }

    /* Extract paths. */
    val src = Paths.get(args[0])
    val dst = Paths.get(args[1])
    System.setProperty("log4j.saveDirectory", dst.parent.resolve("migration.log").toString()) /* Set log path for Log4j2. */

    /* Initialize source store. */
    val store = initializeDatabase(src)

    /* Initializes destination the SQLite database and make it default. */
    val database = Database.connect("jdbc:sqlite:$dst?foreign_keys=on;", driver = "org.sqlite.JDBC")
    TransactionManager.defaultDatabase = database
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE


    /* Initialize schema. */
    Schema.initialize(database)

    /* Perform migration. */
    store.transactional(true) {
        transaction {
            /* Basic entities. */
            DbParticipant.migrate()
            DbInstitution.migrate()
            DbObjectCollection.migrate()
            DbUser.migrate()

            /* Configuration entities. */
            DbSolr.migrate()
            DbCollection.migrate()
            DbImageDeployment.migrate()
            DbEntityMapping.migrate()
            DbAttributeMapping.migrate()
            DbJobTemplate.migrate()
            DbTransformer.migrate()

            /* Migrate Jobs and JobLogs*/
            DbJob.migrate()
            DbJobLog.migrate()

            /* Migrate mapping from institution to collection. */
            DbInstitution.all().asSequence().forEach { dbInstitution ->
                dbInstitution.availableCollections.asSequence().forEach { dbAvailableCollection ->
                    InstitutionsSolrCollections.insert {
                        it[institutionId] = Institutions.idByName(dbInstitution.name)  ?: throw IllegalStateException("Could not find institution with name '${dbInstitution.name}'.")
                        it[solrCollectionId] = SolrCollections.idByName(dbAvailableCollection.name)  ?: throw IllegalStateException("Could not find Apache Solr Collection with name '${dbAvailableCollection.name}'.")
                        it[available] = true
                        it[selected] = dbInstitution.selectedCollections.filter { it.name eq dbAvailableCollection.name }.any()
                    }
                }
            }
        }
    }
}

/**
 * Initializes and returns the [TransientEntityStore] based on the provided [Config].
 *
 * @return [TransientEntityStore]
 */
private fun initializeDatabase(path: Path): TransientEntityStore {
    val store = StaticStoreContainer.init(dbFolder = path.toFile(), entityStoreName = "kiar-db")
    XdModel.registerNodes(
        DbSolr,
        DbCollection,
        DbCollectionType,
        DbJobTemplate,
        DbJobType,
        DbJobSource,
        DbEntityMapping,
        DbAttributeMapping,
        DbAttributeMappingParameters,
        DbRightStatement,
        DbCanton,
        DbFormat,
        DbParser,
        DbTransformer,
        DbTransformerParameter,
        DbTransformerType,
        DbImageFormat,
        DbImageDeployment,
        DbParticipant,
        DbJob,
        DbJobLog,
        DbJobLogLevel,
        DbJobLogContext,
        DbJobStatus,
        DbInstitution,
        DbObjectCollection,
        DbRole,
        DbUser
    )
    initMetaData(XdModel.hierarchy, store)

    return store
}