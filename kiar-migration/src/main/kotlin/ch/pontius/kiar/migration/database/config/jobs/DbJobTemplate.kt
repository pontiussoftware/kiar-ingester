package ch.pontius.kiar.migration.database.config.jobs

import ch.pontius.kiar.migration.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.migration.database.config.solr.DbCollection
import ch.pontius.kiar.migration.database.config.solr.DbSolr
import ch.pontius.kiar.migration.database.config.transformers.DbTransformer
import ch.pontius.kiar.migration.database.institution.DbParticipant
import ch.pontius.kiar.migration.database.job.DbJob
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy

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
}