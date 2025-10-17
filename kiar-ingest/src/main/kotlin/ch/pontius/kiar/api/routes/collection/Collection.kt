package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.collection.PaginatedObjectCollectionResult
import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.collections.Collections
import ch.pontius.kiar.database.collections.Collections.toObjectCollection
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.utilities.ImageHandler
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant

@OpenApi(
    path = "/api/collections",
    methods = [HttpMethod.GET],
    summary = "Retrieves all collections registered in the database.",
    operationId = "getCollections",
    tags = ["Collection"],
    pathParams = [],
    queryParams = [
        OpenApiParam(name = "filter", type = String::class, description = "The filter term for search.", required = false),
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size for pagination.", required = false)],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedObjectCollectionResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getListCollections(ctx: Context) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val filter = ctx.queryParam("filter")?.lowercase()
    val (total, result) = transaction {
        val query = (Collections innerJoin Institutions innerJoin Participants).selectAll()
        if (filter != null) {
            query.andWhere {
                (Collections.name like "$filter%") or  (Collections.displayName like "$filter%")
            }
        }
        query.count() to query.offset((page * pageSize).toLong()).limit(pageSize).asSequence().map { it.toObjectCollection() }.toList()
    }
    ctx.json(PaginatedObjectCollectionResult(total, page, pageSize, result))
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.GET],
    summary = "Gets information about an existing collection.",
    operationId = "getCollection",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection that should be fetched.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(ObjectCollection::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)

fun getCollection(ctx: Context) {
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")

    /* Read collection. */
    val collection = transaction {
        Collections.getById(collectionId) ?: throw ErrorStatusException(400, "Collection with ID $collectionId could not be found.")
    }

    /* Return collection object. */
    ctx.json(collection)
}

@OpenApi(
    path = "/api/collections",
    methods = [HttpMethod.POST],
    summary = "Creates a new collection.",
    operationId = "postCreateCollection",
    tags = ["Collection"],
    requestBody = OpenApiRequestBody([OpenApiContent(ObjectCollection::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postCreateCollection(ctx: Context) {
    val request = ctx.parseBodyOrThrow<ObjectCollection>()

    /* Create new collection. */
    transaction {
        val collectionId = Collections.insertAndGetId { insert ->
            insert[name] = request.name
            insert[displayName] = request.displayName
            insert[description] = request.description
            insert[institutionId] = request.institution?.id ?: throw ErrorStatusException(400, "Must specify a valid institution ID.")
            insert[publish] = request.publish
            insert[filters] = request.filters.toTypedArray()
            insert[images] = request.images.toTypedArray()
        }.value
        request.copy(id = collectionId)
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Collection with ID ${request.id} created successfully."))
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing collection.",
    operationId = "putUpdateCollection",
    tags = ["Collection"],
    requestBody = OpenApiRequestBody([OpenApiContent(ObjectCollection::class)], required = true),
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection that should be updated.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun putUpdateCollection(ctx: Context) {
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val request = ctx.parseBodyOrThrow<ObjectCollection>()

    /* Update collection. */
    val updated = transaction {
        val currentUser = ctx.currentUser()
        val collection = Collections.getById(collectionId)

        /* Make sure, that the current user can actually edit this collection. */
        if (currentUser.role != Role.ADMINISTRATOR && currentUser.institution?.id != collection?.institution?.id) {
            throw ErrorStatusException(403, "Collection with ID $collectionId cannot be edited by current user.")
        }

        /* Update collection. */
        Collections.update({ Collections.id eq collectionId }) { update ->
            update[name] = request.name
            update[displayName] = request.displayName
            update[description] = request.description
            update[publish] = request.publish
            update[filters] = request.filters.toTypedArray()

            if (currentUser.role == Role.ADMINISTRATOR) {
                update[name] = request.name
                update[institutionId] = (request.institution?.id ?: throw ErrorStatusException(400, "Must specify a valid institution ID."))
            }
        }
    }

    /* Return job object. */
    if (updated > 0) {
        ctx.json(SuccessStatus("Collection with ID $collectionId updated successfully."))
    } else {
        ctx.json(ErrorStatus(404, "Collection with ID $collectionId could not be updated because it does not exist."))
    }
}

@OpenApi(
    path = "/api/collections/{id}/{name}",
    methods = [HttpMethod.GET],
    summary = "Gets the preview image for the provided collection.",
    operationId = "getCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection the image should be retrieved for.", required = true),
        OpenApiParam(name = "name", description = "The name of the image.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [
            OpenApiContent(mimeType = "image/jpeg", type = "string", format = "binary"),
            OpenApiContent(mimeType = "image/png", type = "string", format = "binary"),
        ]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getImageForCollection(ctx: Context) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val imageName = ctx.pathParam("name")

    /* Obtain deployment path */
    val deployment = transaction {
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        ImageDeployments.forCollection(collection).firstOrNull() ?: throw ErrorStatusException(404, "No deployment found for institution with ID $collectionId.")
    }

    /* Construct image path. */
    val path = Paths.get(deployment.path).resolve("collections").resolve(deployment.name).resolve(imageName)
    if (!Files.exists(path)) {
        throw ErrorStatusException(404, "No image found for institution with ID $collectionId; missing file.")
    }

    /* Send back image. */
    ctx.status(200)
    when(deployment.format) {
        ImageFormat.JPEG -> ctx.contentType("image/jpeg")
        ImageFormat.PNG -> ctx.contentType("image/png")
    }
    ctx.result(Files.newInputStream(path, StandardOpenOption.READ))
}

@OpenApi(
    path = "/api/collections/{id}/{name}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes the preview image for the provided collection.",
    operationId = "deleteCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection the image should be deleted for.", required = true),
        OpenApiParam(name = "name", description = "The name of the image to delete.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteImageForCollection(ctx: Context) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val imageName = ctx.pathParam("name")

    /* Start transaction and update ecollection. */
    val delete = transaction {
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        val deployments = ImageDeployments.forCollection(collection)
        val newImages = collection.images.toMutableList()

        /* Update collection object. */
        if (newImages.contains(imageName)) {
            newImages -= imageName

            /* Update collection. */
            Collections.update({ Collections.id eq collectionId }) { update ->
                update[images] = newImages.toTypedArray()
                update[modified] = Instant.now()
            }

            /* Obtain deployment path. */
            deployments.map { Paths.get(it.path).resolve("collections").resolve(it.name).resolve(imageName) }.toList()
        } else {
            emptyList()
        }
    }

    /* Delete physical files. */
    for (path in delete) {
        try {
            if (Files.exists(path)) {
                Files.delete(path)
            }
        } catch (_: Throwable) {
            /* No op. */
        }
    }

    /* Set status. */
    ctx.json(SuccessStatus("Image for collection with ID $collectionId deleted successfully."))
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.POST],
    summary = "Posts a new image for the provided collection.",
    operationId = "postCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection the image should be added to.", required = true)
    ],
    requestBody = OpenApiRequestBody(content = [
        OpenApiContent(mimeType = ContentType.FORM_DATA_MULTIPART, properties = [OpenApiContentProperty(name = "image", type = "string", format = "binary")])
    ], description = "The uploaded image file.", required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postUploadImageForCollection(ctx: Context) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val files = ctx.uploadedFiles()

    /* Make sure that a file has been uploaded. */
    if (files.isEmpty()) throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Start transaction */
    val (collection, deployments) = transaction {
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        val deployments = ImageDeployments.forCollection(collection)
        if (deployments.isEmpty()) {
            throw ErrorStatusException(400, "No deployment configuration found for institution-")
        }
        collection to deployments
    }

    /* Define file names. */
    val filename = "${collection.uuid}-${System.currentTimeMillis()}.jpg"

    /* Process images. */
    val newImages = collection.images.toMutableList()
    for (f in ctx.uploadedFiles()) {
        /* Open image. */
        val image = try {
            ImmutableImage.loader().fromStream(f.content())
        } catch (_: IOException) {
            throw ErrorStatusException(400, "Uploaded image file could not be opened.")
        }

        /* Deploy files. */
        for (d in deployments) {
            /* Prepare scaled version. */
            val scaled = if (image.width > image.height) {
                image.scaleToWidth(d.maxSize)
            } else {
                image.scaleToHeight(d.maxSize)
            }
            /* Mark old file for deletion. */
            val path = Paths.get(d.path).resolve("collections").resolve(d.name).resolve(filename)
            try {
                /* Prepare deployment path and create directories if necessary. */
                if (!Files.exists(path.parent)) {
                    Files.createDirectories(path.parent)
                }

                /* Write image. */
                ImageHandler.store(scaled, image.metadata, JpegWriter.Default, path)
            }  catch (_: IOException) {
                throw ErrorStatusException(400, "Could not deploy image.")
            }
        }

        /* Update image names. */
        newImages.add(filename)
    }

    /* Update collection. */
    transaction {
        Collections.update({ Collections.id eq collectionId }) { update ->
            update[images] = newImages.toTypedArray()
            update[modified] = Instant.now()
        }
    }

    /* Set status. */
    ctx.json(SuccessStatus("Images for collection with ID $collectionId uploaded successfully."))
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing collection.",
    operationId = "deleteCollection",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the collection that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteCollection(ctx: Context) {
    val collectionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val (name, images) = transaction {
        val collection = Collections.getById(collectionId) ?: throw ErrorStatusException(400, "Collection with ID $collectionId could not be found.")

        /* Fetch paths to purge. */
        val deployments = ImageDeployments.forCollection(collection)
        val paths = collection.images.flatMap  { i -> deployments.map { d -> Paths.get(d.path).resolve("collections").resolve(d.name).resolve(i) } }

        /* Execute delete. */
        Collections.deleteWhere { Collections.id eq collectionId }

        collection.name to paths
    }

    /* Try to delete associated images. */
    for (path in images) {
        try {
            if (Files.exists(path)) {
                Files.delete(path)
            }
        } catch (_: Throwable) {
            /* No op. */
        }
    }

    /* Return success status. */
    ctx.json(SuccessStatus("Collection '$name' (id: $collectionId) deleted successfully."))
}

/**
 *
 */
private fun ImageDeployments.forCollection(collection: ObjectCollection) = (InstitutionsSolrCollections innerJoin SolrCollections).innerJoin(
    ImageDeployments,
    { SolrCollections.solrInstanceId },
    { ImageDeployments.solrInstanceId }
).select(ImageDeployments.columns).where {
    InstitutionsSolrCollections.institutionId eq collection.institution?.id
}.map {
    it.toImageDeployment()
}



