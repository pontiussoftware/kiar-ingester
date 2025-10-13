package ch.pontius.kiar.migration

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
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    /* Sanity check; is path available. */
    if (args.size != 2) {
        System.err.println("Program expects two arguments: Path to source DB and path to destination DB.")
        exitProcess(1)
    }

    /* Extract paths. */
    val src = Paths.get(args[0])
    Paths.get(args[1])

    /* Initialize source store. */
    initializeDatabase(src)

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