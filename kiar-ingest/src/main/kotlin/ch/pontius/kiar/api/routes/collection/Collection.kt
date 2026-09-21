package ch.pontius.kiar.api.routes.collection

import ch.pontius.kiar.api.model.collection.ObjectCollection
import ch.pontius.kiar.api.model.collection.PaginatedObjectCollectionResult
import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.collections.Collections
import ch.pontius.kiar.database.collections.Collections.toObjectCollection
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.utilities.ImageHandler
import ch.pontius.kiar.utilities.SafeImageLoader
import ch.pontius.kiar.utilities.extensions.*
import com.sksamuel.scrimage.nio.JpegWriter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

val getListCollectionsDoc: RouteDoc = {
    operationId = "getCollections"
    summary = "Retrieves all collections registered in the database."
    tags("Collection")
    parameters {
        queryParam("filter", "The filter term for search.")
        queryParam("page", "The page index (zero-based) for pagination.", INT32)
        queryParam("pageSize", "The page size for pagination.", INT32)
    }
    responses {
        json<PaginatedObjectCollectionResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun getListCollections(call: ApplicationCall) {
    val (page, pageSize) = call.pagination()
    val filter = call.queryParam("filter")?.lowercase()
    val (total, result) = transaction {
        val query = (Collections innerJoin Institutions innerJoin Participants).selectAll()
        if (filter != null) {
            query.andWhere {
                (Collections.name like "$filter%") or  (Collections.displayName like "$filter%")
            }
        }
        query.count() to query.offset(page.toLong() * pageSize).limit(pageSize).asSequence().map { it.toObjectCollection() }.toList()
    }
    call.respond(PaginatedObjectCollectionResult(total, page, pageSize, result))
}

val getCollectionDoc: RouteDoc = {
    operationId = "getCollection"
    summary = "Gets information about an existing collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection that should be fetched.")
    }
    responses {
        json<ObjectCollection>(200)
        errors(400, 401, 403, 404, 500)
    }
}


suspend fun getCollection(call: ApplicationCall) {
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")

    /* Read collection. */
    val collection = transaction {
        val collection = Collections.getById(collectionId) ?: throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        call.currentUser().requireInstitution(collection.institution?.id, "Collection with ID $collectionId cannot be accessed by current user.")
        collection
    }

    /* Return collection object. */
    call.respond(collection)
}

val postCreateCollectionDoc: RouteDoc = {
    operationId = "postCreateCollection"
    summary = "Creates a new collection."
    tags("Collection")
    jsonBody<ObjectCollection>()
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun postCreateCollection(call: ApplicationCall) {
    val request = call.receiveOrThrow<ObjectCollection>()

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
    call.respond(SuccessStatus("Collection with ID ${request.id} created successfully."))
}

val putUpdateCollectionDoc: RouteDoc = {
    operationId = "putUpdateCollection"
    summary = "Updates an existing collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection that should be updated.")
    }
    jsonBody<ObjectCollection>()
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun putUpdateCollection(call: ApplicationCall) {
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val request = call.receiveOrThrow<ObjectCollection>()

    /* Update collection. */
    val updated = transaction {
        val currentUser = call.currentUser()
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
        call.respond(SuccessStatus("Collection with ID $collectionId updated successfully."))
    } else {
        throw ErrorStatusException(404, "Collection with ID $collectionId could not be updated because it does not exist.")
    }
}

val getImageForCollectionDoc: RouteDoc = {
    operationId = "getCollectionImage"
    summary = "Gets the preview image for the provided collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection the image should be retrieved for.")
        pathParam("name", "The name of the image.", STRING)
    }
    responses {
        image(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun getImageForCollection(call: ApplicationCall) {
    /* Obtain parameters. */
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val imageName = call.pathParam("name")

    /* Obtain deployment path */
    val deployment = transaction {
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")

        /* Only serve images registered for this collection; the name is a user-controlled path segment otherwise. */
        if (!collection.images.contains(imageName)) {
            throw ErrorStatusException(404, "No image named '$imageName' found for collection with ID $collectionId.")
        }
        ImageDeployments.forCollection(collection).firstOrNull() ?: throw ErrorStatusException(404, "No deployment found for institution with ID $collectionId.")
    }

    /* Construct image path. */
    val path = Paths.get(deployment.path).resolve("collections").resolve(deployment.name).resolve(imageName)
    if (!Files.exists(path)) {
        throw ErrorStatusException(404, "No image found for institution with ID $collectionId; missing file.")
    }

    /* Send back image. */
    val contentType = when(deployment.format) {
        ImageFormat.JPEG -> ContentType.Image.JPEG
        ImageFormat.PNG -> ContentType.Image.PNG
    }
    call.respond(LocalFileContent(path.toFile(), contentType))
}

val deleteImageForCollectionDoc: RouteDoc = {
    operationId = "deleteCollectionImage"
    summary = "Deletes the preview image for the provided collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection the image should be deleted for.")
        pathParam("name", "The name of the image to delete.", STRING)
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteImageForCollection(call: ApplicationCall) {
    /* Obtain parameters. */
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val imageName = call.pathParam("name")

    /* Start transaction and update ecollection. */
    val delete = transaction {
        val currentUser = call.currentUser()
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")

        /* Make sure, that the current user can actually edit this collection. */
        if (currentUser.role != Role.ADMINISTRATOR && currentUser.institution?.id != collection.institution?.id) {
            throw ErrorStatusException(403, "Collection with ID $collectionId cannot be edited by current user.")
        }
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
    call.respond(SuccessStatus("Image for collection with ID $collectionId deleted successfully."))
}

val postUploadImageForCollectionDoc: RouteDoc = {
    operationId = "postCollectionImage"
    summary = "Posts a new image for the provided collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection the image should be added to.")
    }
    multipartFile("image", "The uploaded image file.")
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun postUploadImageForCollection(call: ApplicationCall) {
    /* Obtain parameters. */
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
    val files = call.uploadedFiles()

    /* Make sure that a file has been uploaded. */
    if (files.isEmpty()) throw ErrorStatusException(401, "Uploaded file is missing.")

    try {

    /* Start transaction */
    val (collection, deployments) = transaction {
        val collection = Collections.getById(collectionId) ?:  throw ErrorStatusException(404, "Collection with ID $collectionId could not be found.")
        call.currentUser().requireInstitution(collection.institution?.id, "Collection with ID $collectionId cannot be edited by current user.")
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
    for (f in files) {
        /* Open image. */
        val image = try {
            SafeImageLoader.load(f.path)
        } catch (e: SafeImageLoader.ImageTooLargeException) {
            throw ErrorStatusException(400, "Uploaded image is too large: ${e.message}")
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
    call.respond(SuccessStatus("Images for collection with ID $collectionId uploaded successfully."))
    } finally {
        files.forEach { it.delete() }
    }
}

val deleteCollectionDoc: RouteDoc = {
    operationId = "deleteCollection"
    summary = "Deletes an existing collection."
    tags("Collection")
    parameters {
        pathParam("id", "The ID of the collection that should be deleted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteCollection(call: ApplicationCall) {
    val collectionId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed collection ID.")
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
    call.respond(SuccessStatus("Collection '$name' (id: $collectionId) deleted successfully."))
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



