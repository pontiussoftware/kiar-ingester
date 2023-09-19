package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.image.ImageDeployment
import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.image.DbImageDeployment
import ch.pontius.kiar.database.config.image.DbImageFormat
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById
import org.joda.time.DateTime

@OpenApi(
    path = "/api/deployment/images/formats",
    methods = [HttpMethod.GET],
    summary = "Lists all available formats available for image deployment.",
    operationId = "getListImageFormats",
    tags = ["Config", "Image Deployment"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ImageFormat>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listFormats(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val parsers = DbImageFormat.all()
        ctx.json(parsers.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/deployment/images",
    methods = [HttpMethod.GET],
    summary = "Lists all available image deployments.",
    operationId = "getListImageDeployments",
    tags = ["Config", "Image Deployment"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ImageDeployment>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listImageDeployments(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val mappings = DbImageDeployment.all()
        ctx.json(mappings.mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/deployment/images",
    methods = [HttpMethod.POST],
    summary = "Creates a new image deployment.",
    operationId = "postCreateImageDeployment",
    tags = ["Config", "Image Deployment"],
    pathParams = [],
    requestBody = OpenApiRequestBody([OpenApiContent(ImageDeployment::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(ImageDeployment::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun createImageDeployment(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(ImageDeployment::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }
    val created = store.transactional {
        /* Update basic properties. */
        val mapping = DbImageDeployment.new {
            name = request.name
            format = request.format.toDb()
            host = request.host
            deployTo = request.deployTo
            maxSize = request.maxSize
            createdAt = DateTime.now()
            changedAt = DateTime.now()

        }
        mapping.toApi()
    }
    ctx.json(created)
}

@OpenApi(
    path = "/api/deployment/images/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing image deployment.",
    operationId = "deleteImageDeployment",
    tags = ["Config", "Image Deployment"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the image deployment that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteImageDeployment(ctx: Context, store: TransientEntityStore) {
    val mappingId = ctx.pathParam("id")
    store.transactional {
        val mapping = try {
            DbImageDeployment.findById(mappingId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Image deployment with ID $mappingId could not be found.")
        }
        mapping.delete()
    }
    ctx.json(SuccessStatus("Image deployment $mappingId deleted successfully."))
}

@OpenApi(
    path = "/api/mappings/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing image deployment.",
    operationId = "updateImageDeployment",
    tags = ["Config", "Image Deployment"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the image deployment that should be updated.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(ImageDeployment::class)], required = true),
    responses = [
        OpenApiResponse("200", [OpenApiContent(ImageDeployment::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateImageDeployment(ctx: Context, store: TransientEntityStore) {
    /* Extract the ID and the request body. */
    val mappingId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(ImageDeployment::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request body.")
    }

    /* Start transaction. */
    val updated = store.transactional {
        val mapping = try {
            DbImageDeployment.findById(mappingId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Image deployment with ID $mappingId could not be found.")
        }

        /* Update basic properties. */
        mapping.name = request.name
        mapping.format = request.format.toDb()
        mapping.host = request.host
        mapping.deployTo = request.deployTo
        mapping.maxSize = request.maxSize
        mapping.changedAt = DateTime.now()

        /* Now merge attribute mappings. */
        mapping.toApi()
    }

    ctx.json(updated)
}

