package ch.pontius.kiar.database.job

import ch.pontius.kiar.api.model.job.Job
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.kiar.ingester.processors.sinks.DummySink
import ch.pontius.kiar.ingester.processors.sinks.Sink
import ch.pontius.kiar.ingester.processors.sources.KiarFileSource
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.sources.XmlFileSource
import ch.pontius.kiar.ingester.processors.transformers.Transformer
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.size
import org.apache.solr.common.SolrInputDocument

/**
 * A [DbJob] as managed by the KIAR Tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJob(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbJob>()

    /** Name of this [DbJob]. */
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The number of items processed by this [DbJob]. */
    var processed by xdLongProp()

    /** The number of items skipped by this [DbJob]. */
    var skipped by xdLongProp()

    /** The number of errors that occurred while processing this [DbJob]. */
    var error by xdLongProp()

    /** The [DbJobStatus] this [DbJob]. */
    var status by xdLink1(DbJobStatus)

    /** The [DbJobStatus] this [DbJob]. */
    var source by xdLink1(DbJobSource)

    /** The [DbJobTemplate] this [DbJob] has been created with. */
    var template by xdLink0_1(DbJobTemplate, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The date and time this [DbJob] was created. */
    var createdAt by xdRequiredDateTimeProp()

    /** The date and time this [DbJob] was last changed. */
    var changedAt by xdDateTimeProp()

    /** The [DbUser] that started this [DbJob]. */
    var createdByName by xdRequiredStringProp {  }

    /** The [DbUser] that started this [DbJob]. */
    var createdBy by xdLink0_1(DbUser)

    /** The [DbJobLog] entries associated with this [DbJob]. */
    val log by xdChildren0_N(DbJobLog::job)

    /**
     * Generates and returns a new [Transformer] instance from this [DbTransformer] entry.
     *
     * Requires an ongoing transactional context!
     *
     * @param config The KIAR tools [Config] object.
     * @return [Transformer]
     */
    fun toPipeline(config: Config): Sink<SolrInputDocument> {
        val template = this.template ?: throw IllegalStateException("Failed to generated execution pipeline for job ${this.xdId}: Missing template.")

        /* Generate file source. */
        val sourcePath = config.ingestPath.resolve(template.participant.name).resolve(this.xdId)
        val source: Source<SolrInputDocument> = when (template.type.description) {
            "XML" -> XmlFileSource(sourcePath, template.mapping.toApi())
            "KIAR" -> KiarFileSource(sourcePath, template.mapping.toApi())
            else -> throw IllegalStateException("Unsupported template type '${template.type.description}'. This is a programmer's error!")
        }
        var root = source

        /* Generate all transformers source. */
        for (t in template.transformers.asSequence()) {
            root = t.newInstance(root)
        }

        /* Return ApacheSolrSink. */
        return DummySink(root)
    }

    /**
     * Convenience method to convert this [DbJob] to a [Job].
     *
     * Requires an ongoing transaction.
     *
     * @return [Job]
     */
    fun toApi() = Job(
        this.xdId,
        this.name,
        this.status.toApi(),
        this.source.toApi(),
        this.template?.toApi(),
        this.processed,
        this.skipped,
        this.error,
        this.log.size(),
        this.createdAt.millis,
        this.changedAt?.millis,
        this.createdByName,
    )
}