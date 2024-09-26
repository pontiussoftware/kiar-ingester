package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.media.MediaProvider
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuid
import com.sksamuel.scrimage.ImmutableImage
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * A [ValueParser] that converts a [String] (path) to a [ImmutableImage]. Involves reading the image from the file system.
 *
 * @author Ralph Gasser
 * @version 2.1.0
 */
class FileImageValueParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {

    /** Reads the search pattern from the parameters map.*/
    private val search: Regex? = this.mapping.parameters["search"]?.let { Regex(it) }

    /** Reads the replacement pattern from the parameters map.*/
    private val replace: String? = this.mapping.parameters["replace"]

    /**
     * Parses the given [String] and resolves it into a [FileImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse or null.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value == null) return

        /* Read path - apply Regex search/replace if needed. */
        val actualPath = if (this.search != null && this.replace != null) {
            value.replace(this.search, this.replace)
        } else {
            value
        }

        /* Parse path and read file. */
        val path = Paths.get(actualPath)
        if (this.mapping.multiValued) {
            into.addField(mapping.destination, FileImageProvider(into.uuid(), path, context))
        } else {
            into.setField(mapping.destination, FileImageProvider(into.uuid(), path, context))
        }
    }

    /**
     * A [MediaProvider.Image] for the images addressed by a [Path].
     */
    private data class FileImageProvider(private val uuid: String, private val path: Path, private val context: ProcessingContext): MediaProvider.Image {
        override fun open(): ImmutableImage? = try {
            Files.newInputStream(this.path, StandardOpenOption.READ).use { ImmutableImage.loader().fromStream(it) }
        } catch (e: Throwable) {
            context.log(JobLog(context.jobId, this.uuid, null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to decode image from '${this.path}'. An exception occurred: ${e.message}"))
            null
        }
    }
}