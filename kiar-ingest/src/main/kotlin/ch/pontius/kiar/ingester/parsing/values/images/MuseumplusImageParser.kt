package ch.pontius.kiar.ingester.parsing.values.images

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.parsing.values.images.providers.URLImageProvider
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.solrj.uuidOrNull
import com.sksamuel.scrimage.ImmutableImage
import org.apache.solr.common.SolrInputDocument
import java.net.URI
import java.net.URL

/**
 * A [ValueParser] that converts a [String] (ID) into a [ImmutableImage] that is downloaded via the zetcom / museumPlus API.
 *
 * See [museumPlus API Documentation](http://docs.zetcom.com/framework-public/ws/ws-api-module.html#get-the-thumbnail-of-a-module-item-attachment)
 *
 * @author Ralph Gasser
 * @version 3.0.0
 */
class MuseumplusImageParser(override val mapping: AttributeMapping): ValueParser<List<ImmutableImage>> {
    /** The separator used for splitting. */
    private val delimiter: String = this.mapping.parameters["delimiter"] ?: ","

    /** Reads the search pattern from the parameters map.*/
    private val host: String = this.mapping.parameters["host"] ?: throw IllegalStateException("Host required but missing.")

    /** Reads the username from the parameters map. This is required! */
    private val username: String = this.mapping.parameters["username"]  ?: throw IllegalStateException("Username required but missing.")

    /** Reads the password from the parameters map. This is required! */
    private val password: String = this.mapping.parameters["password"] ?: throw IllegalStateException("Password required but missing.")

    /** Reads the replacement pattern from the parameters map.*/
    private val mode: Mode = this.mapping.parameters["mode"]?.let {
        Mode.valueOf(it.uppercase())
    } ?: Mode.ID

    /**
     * Parses the given [String] and resolves it into a [MuseumplusImageProvider] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @param into The [SolrInputDocument] to append the value to.
     * @param context The [ProcessingContext]
     */
    override fun parse(value: String?, into: SolrInputDocument, context: ProcessingContext) {
        if (value.isNullOrEmpty()) return

        /* Read values. */
        val urls = when (this.mode) {
            Mode.ID -> this.urlFromId(value)
            Mode.PATH -> this.urlFromPath(value)
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

    /**
     * Parses the given [String] and resolves it into a [URL] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @return [List] of [URL]s.
     */
    private fun urlFromId(value: String) = value.split(this.delimiter).mapNotNull {
        it.trim().toBigDecimalOrNull()?.toInt()
    }.map { id -> URI("${this.host}/ria-ws/application/module/Multimedia/$id/thumbnail?size=EXTRA_EXTRA_LARGE").toURL() }

    /**
     * Parses the given [String] and resolves it into a [URL] the provided [SolrInputDocument].
     *
     * @param value The [String] value to parse.
     * @return [List] of [URL]s.
     */
    private fun urlFromPath(value: String) = value.split(this.delimiter).map {
        URI(this.host + (if (this.host.endsWith("/") || it.startsWith("/")) "" else "/") + it.trim()).toURL()
    }

    /**
     * Returns the [Mode] for the provided [String].
     */
    private enum class Mode {
        ID, PATH
    }
}