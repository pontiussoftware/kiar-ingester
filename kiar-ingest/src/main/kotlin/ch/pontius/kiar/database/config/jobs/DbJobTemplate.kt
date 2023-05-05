package ch.pontius.kiar.database.config.jobs

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.institution.DbParticipant
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import java.nio.file.Path

/**
 * The template for a job used by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobTemplate(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbJobTemplate>()

    /** The name of this [DbJobTemplate]. */
    val name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbJobType] of this [DbJobTemplate]. */
    val type by xdLink1(DbJobType)

    /** The [DbParticipant] this [DbJobTemplate] belongs to. */
    val participant by xdLink1(DbParticipant)

    /** The [DbCollection]s this [DbJobTemplate] maps to. */
    val collections by xdLink0_N(DbCollection)

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    val mapping: DbEntityMapping by xdChild1(DbEntityMapping::template)

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    val transformers by xdChildren0_N(DbTransformer::template)

    /** Flag indicating, if this [DbJobTemplate] should be started automatically once the file appears. */
    val startAutomatically by xdBooleanProp()

    /** Flag indicating, if this [DbJobTemplate] should be started automatically once the file appears. */
    val deleted by xdBooleanProp()

    /**
     * Returns the [Path] to the expected ingest file for this [DbJobTemplate].
     *
     * Requires an ongoing transaction!
     */
    fun sourcePath(config: Config): Path = config.ingestPath.resolve(this.participant.name).resolve("${this.name} + ${this.type.suffix}")
}