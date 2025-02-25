package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.collection.PaginatedObjectCollectionResult
import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.database.collection.DbObjectCollection
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.utilities.ImageHandler
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

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
fun getListCollections(ctx: Context, store: TransientEntityStore) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val filter = ctx.queryParam("filter")?.lowercase()
    val (total, result) = store.transactional(true) {
        val query =  if (filter != null) {
            DbObjectCollection.filter { (it.name.startsWith(filter)) or (it.displayName.startsWith(filter)) }
        } else {
            DbObjectCollection.all()
        }
        query.size() to query.drop(page * pageSize).take(pageSize).asSequence().map { it.toApi() }.toList()
    }
    ctx.json(PaginatedObjectCollectionResult(total, page, pageSize, result))
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
fun postCreateCollection(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(ObjectCollection::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Create new collection. */
    val collection = store.transactional {
        val institution = DbInstitution.filter { it.name eq request.institutionName }.firstOrNull() ?: throw ErrorStatusException(404, "Institution '${request.institutionName}' could not be found.")
        DbObjectCollection.new {
            this.name = request.name
            this.displayName = request.displayName
            this.description = request.description
            this.publish = request.publish
            this.filters = request.filters.toSet()
            this.institution = institution
        }.toApi()
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Collection '${collection.name}' (id: ${collection.id}) created successfully."))
}
@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.GET],
    summary = "Gets information about an existing collection.",
    operationId = "getCollection",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection that should be fetched.", required = true)
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

fun getCollection(ctx: Context, store: TransientEntityStore) {
    val collectionId = ctx.pathParam("id")

    /* Read collection. */
    val collection = store.transactional(true) {
        try {
            DbObjectCollection.findById(collectionId).toApi()
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }
    }

    /* Return job object. */
    ctx.json(collection)
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing collection.",
    operationId = "putUpdateCollection",
    tags = ["Collection"],
    requestBody = OpenApiRequestBody([OpenApiContent(ObjectCollection::class)], required = true),
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection that should be updated.", required = true)
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
fun putUpdateCollection(ctx: Context, store: TransientEntityStore) {
    val collectionId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(ObjectCollection::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Update collection. */
    val collectionName = store.transactional {
        val collection = try {
            DbObjectCollection.findById(collectionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }

        /* Make sure, that the current user can actually edit this collection. */
        val currentUser = ctx.currentUser()
        if (currentUser.role != DbRole.ADMINISTRATOR && currentUser.institution != collection.institution) {
            throw ErrorStatusException(403, "Collection with ID $collectionId cannot be edited by current user.")
        }

        /* Update institution. */
        collection.displayName = request.displayName
        collection.description = request.description
        collection.publish = request.publish
        collection.filters = request.filters.toSet()

        /* Some data can only be edited by an administrator. */
        if (currentUser.role == DbRole.ADMINISTRATOR) {
            collection.name = request.name
            collection.institution = DbInstitution.filter { it.name eq request.institutionName }.firstOrNull() ?: throw ErrorStatusException(404, "Institution '${request.institutionName}' could not be found.")
        }

        collection.name
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Collection '$collectionName' (id: $collectionId) updated successfully."))
}

@OpenApi(
    path = "/api/collections/{id}/{name}",
    methods = [HttpMethod.GET],
    summary = "Gets the preview image for the provided collection.",
    operationId = "getCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection the image should be retrieved for.", required = true),
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
fun getImageForCollection(ctx: Context, store: TransientEntityStore) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id")
    val imageName = ctx.pathParam("name")

    /* Start transaction */
    store.transactional(true) {
        val collection = try {
            DbObjectCollection.findById(collectionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }

        /* Obtain deployment path. */
        val deployment = collection.institution.availableCollections.mapDistinct { it.solr }.flatMapDistinct { it.deployments }.asSequence().map { it.toApi() }.firstOrNull() ?: throw ErrorStatusException(404, "No deployment found for institution with ID $collectionId.")

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
}

@OpenApi(
    path = "/api/collections/{id}/{name}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes the preview image for the provided collection.",
    operationId = "deleteCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection the image should be deleted for.", required = true),
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
fun deleteImageForCollection(ctx: Context, store: TransientEntityStore) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id")
    val imageName = ctx.pathParam("name")

    /* Start transaction and update ecollection. */
    val paths = store.transactional {
        val collection = try {
            DbObjectCollection.findById(collectionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }

        /* Update collection object. */
        if (collection.images.contains(imageName)) {
            collection.images -= imageName

            /* Obtain deployment path. */
            collection.institution.availableCollections.mapDistinct { it.solr }.flatMapDistinct { it.deployments }.asSequence().map {
                Paths.get(it.path).resolve("collections").resolve(it.name).resolve(imageName)
            }.toList()
        } else {
            emptyList()
        }
    }

    /* Delete physical files. */
    for (path in paths) {
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }

    /* Set status. */
    ctx.json(SuccessStatus("Image '$imageName' (id: $collectionId) deleted successfully."))
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.POST],
    summary = "Posts a new image for the provided collection.",
    operationId = "postCollectionImage",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection the image should be added to.", required = true)
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
fun postUploadImageForCollection(ctx: Context, store: TransientEntityStore) {
    /* Obtain parameters. */
    val collectionId = ctx.pathParam("id")
    val files = ctx.uploadedFiles()

    /* Make sure that a file has been uploaded. */
    if (files.isEmpty()) throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Start transaction */
    store.transactional {
        val collection = try {
            DbObjectCollection.findById(collectionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }

        /* List of deployment paths. */
        val deployments = collection.institution.availableCollections.mapDistinct {
            it.solr
        }.flatMapDistinct {
            it.deployments
        }.asSequence().map { it.toApi() }.toList()
        if (deployments.isEmpty()) {
            throw ErrorStatusException(400, "No deployment configuration found for institution-")
        }

        /* Define file names. */
        val filename = "${collection.xdId}-${System.currentTimeMillis()}.jpg"

        /* Process images. */
        for (f in ctx.uploadedFiles()) {
            /* Open image. */
            val image = try {
                ImmutableImage.loader().fromStream(f.content())
            } catch (e: IOException) {
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
                }  catch (e: IOException) {
                    throw ErrorStatusException(400, "Could not deploy image.")
                }
            }

            /* Update image names. */
            val newImages = collection.images.toMutableList()
            newImages.add(filename)
            collection.images = newImages.toSet()

            /* One image is enough. */
            break
        }
    }
}

@OpenApi(
    path = "/api/collections/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing collection.",
    operationId = "deleteCollection",
    tags = ["Collection"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the collection that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteCollection(ctx: Context, store: TransientEntityStore) {
    val collectionId = ctx.pathParam("id")
    val (name, images) = store.transactional {
        val collection = try {
            DbObjectCollection.findById(collectionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        }
        val name = collection.name

        /* Obtain deployment path. */
        val deployments = collection.institution.availableCollections.mapDistinct { it.solr }.flatMapDistinct { it.deployments }.asSequence().map { it.toApi() }.toList()

        /* Return name and images. */
        val ret = name to collection.images.flatMap { i -> deployments.map { d -> Paths.get(d.path).resolve("collections").resolve(d.name).resolve(i) } }
        collection.delete()
        ret
    }

    /* Try to delete associated images. */
    try {
        for (path in images) {
            if (Files.exists(path)) {
                Files.delete(path)
            }
        }
    } catch (e: Throwable) {
        // TODO: Log error
    } finally {
        ctx.json(SuccessStatus("Collection '$name' (id: $collectionId) deleted successfully."))
    }
}



