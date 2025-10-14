package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.parsing.values.images.providers.URLImageProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import com.sksamuel.scrimage.ImmutableImage
import org.apache.solr.common.SolrInputDocument
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * A [ValueParser] that converts a [URL] [String] [ImmutableImage] downloaded from a URL.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class URLImageValueParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {

    /** The separator used for splitting. */
    private val delimiter: String = this.mapping.parameters["delimiter"] ?: ","

    /** Reads the host name from the parameters map. If set, then the URL will be prefixed.*/
    private val host: String? = this.mapping.parameters["host"]

    /** Reads the optional username from the parameters map. If set, HTTP basic authorization will be used to access the resource. */
    private val username: String? = this.mapping.parameters["username"]

    /** Reads the optional password from the parameters map. If set, HTTP basic authorization will be used to access the resource.  */
    private val password: String? = this.mapping.parameters["password"]

    /**
     * Parses the given [String] and resolves it into a [URLImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return

        /* Read values. */
        val urls = value.split(this.delimiter).mapNotNull {
            val str = if (this.host.isNullOrEmpty()) {
               URLEncoder.encode(it.trim(), "UTF-8")
            } else {
               URLEncoder.encode(this.host + (if (this.host.endsWith("/") || it.startsWith("/")) "" else "/") + it.trim(), "UTF-8")
            }

            try {
                URI(str).toURL()
            } catch (e: Throwable) {
                context.log(JobLog(context.jobId, into.uuidOrNull(), null, JobLogContext.RESOURCE, JobLogLevel.WARNING, "Failed to parse URL '$str'; ${e.message}."))
                null
            }
        }

        /* Process URls. */
        for (url in urls) {
            val provider = URLImageProvider(into.uuidOrNull(), url, context, this.username, this.password)
            if (this.mapping.multiValued) {
                into.addField(this.mapping.destination, provider)
            } else {
                into.setField(this.mapping.destination, provider)
            }
        }
    }
}