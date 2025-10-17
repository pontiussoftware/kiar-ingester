package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.kiar.ingester.processors.sinks.Sink
import ch.pontius.kiar.ingester.processors.sources.*
import ch.pontius.kiar.ingester.processors.transformers.ImageDeployment
import kotlinx.serialization.Serializable
import org.apache.solr.common.SolrInputDocument

typealias JobId = Int

/**
 * A data ingest [Job]]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class Job(
    /** The (optional) database [JobId] of this [Job]. */
    val id: JobId? = null,

    /** The name of this [Job]. */
    val name: String,

    /** The [JobStatus] this [Job]. */
    val status: JobStatus,

    /** The [JobSource] this [Job]. */
    val source: JobSource,

    /** The [JobTemplate] of this [Job]. */
    val template: JobTemplate? = null,

    /** The entries processed by this [Job]. */
    var processed: Long = 0L,

    /** The entries skipped by this [Job]. */
    var skipped: Long = 0L,

    /** The number of processing errors encountered by this [Job]. */
    var error: Long = 0L,

    /** The number of [JobLog] entries for this [Job]. */
    var logEntries: Long = 0,

    /** Timestamp of this [Job]'s creation. */
    val createdAt: Long,

    /** Timestamp of this [Job]'s last change. */
    val changedAt: Long? = null,

    /** Name of the user who created this [Job]. */
    val createdBy: String,
) {
    /**
     * Generates and returns a new data ingest pipeline from this [Job].
     *
     * Requires an ongoing transactional context!
     *
     * @param config The KIAR tools [Config] object.
     * @param context The [ProcessingContext] for which to create the pipeline
     * @return [Sink] representing the pipeline.
     */
    fun toPipeline(config: Config): Sink<SolrInputDocument> {
        require(this.template != null) { "Job template is required in order to construct a processing pipeline." }

        /* Generate file source. */
        val sourcePath = config.ingestPath.resolve(this.template.participantName).resolve("${this.id}.job")
        val source: Source<SolrInputDocument> = when (this.template.type) {
            JobType.XML -> XmlFileSource(sourcePath)
            JobType.JSON -> JsonFileSource(sourcePath)
            JobType.KIAR -> KiarFileSource(sourcePath)
            JobType.EXCEL -> ExcelFileSource(sourcePath)
        }
        var root = source

        /* Generate all transformers. */
        for (t in this.template.transformers.asSequence()) {
            root = t.newInstance(root)
        }

        /* Create image deployment stage. */
        root = ImageDeployment(root)

        /* Return ApacheSolrSink. */
        return ApacheSolrSink(root)
    }
}