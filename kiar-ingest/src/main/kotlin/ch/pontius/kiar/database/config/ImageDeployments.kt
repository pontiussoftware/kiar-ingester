package ch.pontius.kiar.database.config

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.image.ImageFormat
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/**
 * A [IntIdTable] that holds information about [ImageDeployments].
 *
 * [ImageDeployments] configure how ingested images are being deployed.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object ImageDeployments: IntIdTable("image_deployments") {
    /** Reference to the [SolrConfigs] entry a [ImageDeployments] belongs to. */
    val solrInstanceId = reference("solr_instance_id", SolrConfigs, ReferenceOption.CASCADE)

    /** The name of the [ImageDeployments] entry. */
    val name = varchar("name", 255).uniqueIndex()

    /** The [ImageFormat] used by a [ImageDeployments] entry. */
    val format = enumerationByName("format", 16, ImageFormat::class)

    /** The source field used by an ImageDeployments] entry. */
    val src = varchar("source", 255)

    /** The (local) image deployment path used by an [ImageDeployments] entry. */
    val path =  varchar("path", 255)

    /** The URL of the image deployment server used by an [ImageDeployments] entry. */
    val server = varchar("server", 255).nullable()

    /** The maximum size used by an [ImageDeployments] entry. */
    val maxSize = integer("max_size")

    /**
     * Converts this [ResultRow] into an [ImageDeployments].
     *
     * @return [ImageDeployments]
     */
    fun ResultRow.toImageDeployment() = ImageDeployment(
        id = this[id].value,
        name = this[name],
        format = this[format],
        source = this[src],
        path = this[path],
        server = this[server],
        maxSize = this[maxSize],
    )
}