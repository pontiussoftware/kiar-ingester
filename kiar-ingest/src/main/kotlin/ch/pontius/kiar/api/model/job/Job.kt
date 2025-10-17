package ch.pontius.kiar.api.model.job

import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.kiar.ingester.processors.sinks.DummySink
import ch.pontius.kiar.ingester.processors.sinks.Sink
import ch.pontius.kiar.ingester.processors.sources.*
import ch.pontius.kiar.ingester.processors.transformers.ImageDeployment
import kotlinx.serialization.Serializable
import org.apache.solr.common.SolrInputDocument
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

typealias JobId = Int

/**
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
     * @param test Flag indicating whether this pipeline is for testing purposes.
     * @return [Sink] representing the pipeline.
     */
    fun toPipeline(config: Config, test: Boolean = false): Sink<SolrInputDocument> {
        val template = this.template ?: throw IllegalStateException("Failed to generated execution pipeline for job ${this.id}: Missing template.")
        val mapping = template.mapping ?: throw IllegalStateException("Failed to generated execution pipeline for job ${this.id}: Missing entity mapping.")
        val solrConfig = template.config ?: throw IllegalStateException("Failed to generated execution pipeline for job ${this.id}: Missing Apache Solr configuration.")

        /* Generate file source. */
        val sourcePath = config.ingestPath.resolve(template.participantName).resolve("${this.id}.job")
        val source: Source<SolrInputDocument> = when (template.type) {
            JobType.XML -> XmlFileSource(sourcePath, mapping)
            JobType.JSON -> JsonFileSource(sourcePath, mapping)
            JobType.KIAR -> KiarFileSource(sourcePath, mapping)
            JobType.EXCEL -> ExcelFileSource(sourcePath, mapping)
        }
        var root = source

        /* Generate all transformers. */
        for (t in template.transformers.asSequence()) {
            root = t.newInstance(root)
        }

        /* Create image deployment stage (if necessary). */
        val deployments = ImageDeployments.selectAll().where { ImageDeployments.solrInstanceId eq solrConfig.id }.map { it.toImageDeployment() }
        if (deployments.isNotEmpty()) {
            root = ImageDeployment(root, deployments, test)
        }

        /* Return ApacheSolrSink. */
        return if (test) {
            DummySink(root)
        } else {
            ApacheSolrSink(root, solrConfig)
        }
    }
}