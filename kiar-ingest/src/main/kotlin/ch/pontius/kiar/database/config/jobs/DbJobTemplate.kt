package ch.pontius.kiar.database.config.jobs

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.job.DbJob
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
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
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** An optional description of this [DbJobTemplate]. */
    var description by xdStringProp(trimmed = true)

    /** The [DbJobType] of this [DbJobTemplate]. */
    var type by xdLink1(DbJobType)

    /** Flag indicating, if this [DbJobTemplate] should be started automatically once the file appears. */
    var startAutomatically by xdBooleanProp()

    /** The [DbParticipant] this [DbJobTemplate] belongs to. */
    var participant by xdLink1(DbParticipant)

    /** The [DbCollection]s this [DbJobTemplate] maps to. */
    var solr by xdLink1(DbSolr)

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    var mapping: DbEntityMapping by xdLink1(DbEntityMapping)

    /** The date and time this [DbJobTemplate] was created. */
    var createdAt by xdDateTimeProp()

    /** The date and time this [DbJobTemplate] was last changed. */
    var changedAt by xdDateTimeProp()

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    val transformers by xdChildren0_N(DbTransformer::template)

    /** The {@link DbJobs} that inherit from this {@link DbJobTemplate}. */
    val jobs by xdLink0_N(DbJob::template, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Returns the [Path] to the expected ingest file for this [DbJobTemplate].
     *
     * Requires an ongoing transaction!
     */
    fun sourcePath(config: Config): Path = config.ingestPath.resolve(this.participant.name).resolve("${this.name}.${this.type.suffix}")

    /**
     * Convenience method to convert this [DbJobType] to a [JobType].
     *
     * Requires an ongoing transaction.
     *
     * @return [JobType]
     */
    fun toApi(full: Boolean = false) = JobTemplate(
        this.xdId,
        this.name,
        this.description,
        this.type.toApi(),
        this.startAutomatically,
        this.participant.name,
        this.solr.name,
        this.mapping.name,
        this.changedAt?.millis,
        this.changedAt?.millis,
        if (full) {
            this.transformers.asSequence().map { it.toApi() }.toList()
        } else {
            emptyList()
        }
    )
}