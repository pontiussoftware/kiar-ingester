package ch.pontius.kiar.migration.database.config.jobs

import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.database.config.EntityMappings
import ch.pontius.kiar.database.config.JobTemplates
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.migration.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.migration.database.config.solr.DbCollection
import ch.pontius.kiar.migration.database.config.solr.DbSolr
import ch.pontius.kiar.migration.database.config.transformers.DbTransformer
import ch.pontius.kiar.migration.database.institution.DbParticipant
import ch.pontius.kiar.migration.database.job.DbJob
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.Instant

/**
 * The template for a job used by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobTemplate(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbJobTemplate>(){
        fun migrate() {
            all().asSequence().forEach { dbJobTemplate ->
                JobTemplates.insert {
                    it[participantId] = Participants.idByName(dbJobTemplate.participant.name) ?: throw IllegalStateException("Could not find participant with name '${dbJobTemplate.participant.name}'.")
                    it[entityMappingId] = EntityMappings.idByName(dbJobTemplate.mapping.name) ?: throw IllegalStateException("Could not find entity mapping with name '${dbJobTemplate.mapping.name}'.")
                    it[solrId] = SolrConfigs.idByName(dbJobTemplate.solr.name) ?: throw IllegalStateException("Could not find Apache Solr Config with name '${dbJobTemplate.solr.name}'.")
                    it[name] = dbJobTemplate.name
                    it[description] = dbJobTemplate.description
                    it[type] = JobType.valueOf(dbJobTemplate.type.description)
                    it[startAutomatically] = dbJobTemplate.startAutomatically
                    it[created] = dbJobTemplate.createdAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                    it[modified] = dbJobTemplate.changedAt?.millis?.let { m -> Instant.ofEpochMilli(m) } ?: Instant.now()
                }
            }
        }
    }

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