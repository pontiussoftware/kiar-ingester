package ch.pontius.kiar.api.routes.institution

import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.institution.PaginatedInstitutionResult
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.database.config.ImageDeployments
import ch.pontius.kiar.database.config.ImageDeployments.toImageDeployment
import ch.pontius.kiar.database.config.SolrCollections
import ch.pontius.kiar.database.config.SolrConfigs
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Institutions.toInstitution
import ch.pontius.kiar.database.institutions.InstitutionsSolrCollections
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.Geocoding
import ch.pontius.kiar.utilities.ImageHandler
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant

@OpenApi(
    path = "/api/institutions",
    methods = [HttpMethod.GET],
    summary = "Retrieves all institutions registered in the database.",
    operationId = "getInstitutions",
    tags = ["Institution"],
    pathParams = [],
    queryParams = [
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size for pagination.", required = false),
        OpenApiParam(name = "order", type = String::class, description = "The attribute to order by. Possible values are 'name', 'city', 'zip', 'canton' and 'publish'.", required = false),
        OpenApiParam(name = "orderDir", type = String::class, description = "The sort order. Possible values are 'asc' and 'desc'.", required = false),
        OpenApiParam(name = "filter", type = String::class, description = "A user-defined filter to search for institutions.", required = false)
   ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedInstitutionResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getListInstitutions(ctx: Context) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val order = ctx.queryParam("order")?.lowercase() ?: "name"
    val orderDir = ctx.queryParam("orderDir")?.uppercase()?.let { SortOrder.valueOf(it) } ?: SortOrder.ASC
    val filter = ctx.queryParam("filter")
    val (total, results) = transaction {

        var query = Institutions.selectAll()
        if (filter != null) {
            query.andWhere {
                (Institutions.name like "${filter}%") or (Institutions.displayName like "${filter}%") or (Institutions.city like "${filter}%")
            }
        }

        /* Parse sort order. */
        query = when(order) {
            "city" -> query.orderBy(Institutions.city, orderDir)
            "zip" -> query.orderBy(Institutions.zip, orderDir)
            "canton" -> query.orderBy(Institutions.canton, orderDir)
            "publish" -> query.orderBy(Institutions.publish, orderDir)
            else -> query.orderBy(Institutions.name, orderDir)
        }

        /* Execute query and return paginated result. */
        query.count() to query.drop(page * pageSize).take(pageSize).asSequence().map { it.toInstitution() }.toList()
    }
    ctx.json(PaginatedInstitutionResult(total, page, pageSize, results))
}

@OpenApi(
    path = "/api/institutions/name",
    methods = [HttpMethod.GET],
    summary = "Retrieves all institution names registered in the database.",
    operationId = "getInstitutionNames",
    tags = ["Institution"],
    pathParams = [],
    queryParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getListInstitutionNames(ctx: Context) {
    val list = transaction {
        Institutions.select(Institutions.name).map { it[Institutions.name] }.toTypedArray()
    }
    ctx.json(list)
}

@OpenApi(
    path = "/api/institutions",
    methods = [HttpMethod.POST],
    summary = "Creates a new institution.",
    operationId = "postCreateInstitution",
    tags = ["Institution"],
    requestBody = OpenApiRequestBody([OpenApiContent(Institution::class)], required = true),
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
fun postCreateInstitution(ctx: Context) {
    val request = ctx.parseBodyOrThrow<Institution>()

    /* Create new institution. */
    val institution = transaction {
        val institutionId = Institutions.insertAndGetId { new ->
            new[name] = request.name
            new[displayName] = request.displayName
            new[description] = request.description
            new[isil] = request.isil
            new[street] = request.street
            new[city] = request.city
            new[zip] = request.zip
            new[canton] = request.canton
            new[publish] = request.publish
            new[email] = request.email
            new[homepage] = request.homepage
            new[canton] = request.canton

            /* Set participant by lookup. */
            new[participantId] = Participants.select(Participants.id).where {
                Participants.name eq request.participantName
            }.map {
                it[Participants.id]
            }.firstOrNull() ?: throw ErrorStatusException(404, "Participant ${request.participantName} could not be found.")

            /* Store longitude and latitude. */
            if (request.longitude == null || request.latitude == null) {
                val geocoding = Geocoding.geocode(request.street, request.city, request.zip)
                if (geocoding != null) {
                    new[longitude] = geocoding.lon
                    new[latitude] = geocoding.lat
                }
            } else {
                new[longitude] = request.longitude!!
                new[latitude] = request.latitude!!
            }
        }.value

        /* Create and connect available collections. */
        for (collection in request.availableCollections) {
            val selected = request.selectedCollections.contains(collection)
            InstitutionsSolrCollections.insert { insert ->
                insert[InstitutionsSolrCollections.institutionId] = institutionId
                insert[InstitutionsSolrCollections.solrCollectionId] = SolrCollections.select(SolrCollections.id).where { SolrCollections.name eq collection }.map { it[SolrCollections.id] }.first()
                insert[InstitutionsSolrCollections.selected] = selected
            }
        }
        request.copy(id = institutionId)
    }

    /* Return institution object. */
    ctx.json(institution)
}
@OpenApi(
    path = "/api/institutions/{id}",
    methods = [HttpMethod.GET],
    summary = "Gets information about an existing institution.",
    operationId = "getInstitution",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the institution that should be fetched.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Institution::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)

fun getInstitution(ctx: Context) {
    val institutionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400,"Malformed institution ID.")

    /* Fetch institution */
    val institution = transaction {
        val institution = Institutions.selectAll().where {
            Institutions.id eq institutionId
        }.map { it.toInstitution() }.firstOrNull() ?: throw ErrorStatusException(
            404,
            "Institution with ID $institutionId could not be found."
        )

        /* Fetches available and active collections. */
        val availableCollections = (InstitutionsSolrCollections innerJoin SolrCollections)
            .select(SolrCollections.name)
            .where { (Institutions.id eq institutionId) and (InstitutionsSolrCollections.available eq true) }
            .map { it[SolrCollections.name] }

        val selectedCollections = (InstitutionsSolrCollections innerJoin SolrCollections)
            .select(SolrCollections.name)
            .where { (Institutions.id eq institutionId) and (InstitutionsSolrCollections.available eq true) and (InstitutionsSolrCollections.selected eq true) }
            .map { it[SolrCollections.name] }

        /* Returns institution with collections. */
        institution.copy(availableCollections = availableCollections, selectedCollections = selectedCollections)
    }

    /* Return the institution object. */
    ctx.json(institution)
}

@OpenApi(
    path = "/api/institutions/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing institution.",
    operationId = "putUpdateInstitution",
    tags = ["Institution"],
    requestBody = OpenApiRequestBody([OpenApiContent(Institution::class)], required = true),
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the institution that should be updated.", required = true)
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
fun putUpdateInstitution(ctx: Context) {
    val institutionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400,"Malformed institution ID.")
    val request = ctx.parseBodyOrThrow<Institution>()

    /* Update existing institution object. */
    transaction {
        val institutionName = Institutions.select(Institutions.name).where {
            Institutions.id eq institutionId
        }.map {
            it[Institutions.name]
        }.firstOrNull() ?: throw ErrorStatusException(404,"Institution with ID $institutionId could not be found.")

        /* Make sure, that the current user can actually edit this institution. */
        val currentUser = ctx.currentUser()
        if (currentUser.role != Role.ADMINISTRATOR && currentUser.institution?.name != institutionName) {
            throw ErrorStatusException(403, "Institution with ID $institutionId cannot be edited by current user.")
        }

        /* Perform update . */
        Institutions.update({ Institutions.id eq institutionId }) { update ->
            update[displayName] = request.displayName
            update[description] = request.description
            update[isil] = request.isil
            update[street] = request.street
            update[zip] = request.zip
            update[email] = request.email
            update[homepage] = request.homepage
            update[defaultCopyright] = request.defaultCopyright
            update[defaultRightsStatement] = request.defaultRightStatement
            update[defaultObjectUrl] = request.defaultObjectUrl

            /* Some data can only be edited by an administrator. */
            if (currentUser.role == Role.ADMINISTRATOR) {
                update[name] = request.name
                update[participantId] = Participants.select(Participants.id).where {
                    Participants.name eq request.participantName
                }.map {
                    it[Participants.id]
                }.firstOrNull() ?: throw ErrorStatusException(404, "Participant ${request.participantName} could not be found.")
                update[canton]  = request.canton
                update[publish]  = request.publish
            }

            /* Update longitude and latitude. */
            if (request.longitude == null || request.latitude == null) {
                val geocoding = Geocoding.geocode(request.street, request.city, request.zip)
                if (geocoding != null) {
                    update[longitude] = geocoding.lon
                    update[latitude] = geocoding.lat
                }
            } else {
                update[longitude] = request.longitude!!
                update[latitude] = request.latitude!!
            }

            /* Update modification date. */
            update[modified] = Instant.now()
        }

        /* Clear and reconnect available collections. */
        if (currentUser.role == Role.ADMINISTRATOR) {
            InstitutionsSolrCollections.deleteWhere { InstitutionsSolrCollections.institutionId eq institutionId }
            for (collection in request.availableCollections) {
                val selected = request.selectedCollections.contains(collection)
                InstitutionsSolrCollections.insert { insert ->
                    insert[InstitutionsSolrCollections.institutionId] = institutionId
                    insert[InstitutionsSolrCollections.solrCollectionId] = SolrCollections.select(SolrCollections.id).where { SolrCollections.name eq collection }.map { it[SolrCollections.id] }.first()
                    insert[InstitutionsSolrCollections.selected] = selected
                }
            }
        }
    }

    /* Return success status. */
    ctx.json(SuccessStatus("Institution with ID $institutionId updated successfully."))
}

@OpenApi(
    path = "/api/institutions/{id}/image",
    methods = [HttpMethod.GET],
    summary = "Gets the preview image for the provided institution.",
    operationId = "getInstitutionImage",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the institution the image should be retrieved for.", required = true)
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
fun getImageForInstitution(ctx: Context) {
    /* Obtain parameters. */
    val institutionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400,"Malformed institution ID.")

    /* Start transaction */
    val (imageName, deployment) = transaction {
        val imageName = Institutions.select(Institutions.imageName).where { Institutions.id eq institutionId }
            .map { it[Institutions.imageName] }.firstOrNull() ?: throw ErrorStatusException(
            404,
            "No image found for institution with ID $institutionId."
        )
        val deployment = ImageDeployments.selectAll().where {
            ImageDeployments.solrInstanceId inSubQuery (InstitutionsSolrCollections innerJoin SolrCollections innerJoin SolrConfigs).select(
                SolrConfigs.id
            ).where {
                (InstitutionsSolrCollections.institutionId) eq institutionId and (InstitutionsSolrCollections.selected eq true)
            }
        }.map {
            it.toImageDeployment()
        }.firstOrNull() ?: throw ErrorStatusException(
            404,
            "No deployment found for institution with ID $institutionId."
        )
        imageName to deployment
    }

    /* Construct image path. */
    val path = Paths.get(deployment.path).resolve("institutions").resolve(deployment.name).resolve(imageName)
    if (!Files.exists(path)) {
        throw ErrorStatusException(404, "No image found for institution with ID $institutionId; missing file.")
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
    path = "/api/institutions/{id}/image",
    methods = [HttpMethod.POST],
    summary = "Posts a new image for the provided institution.",
    operationId = "postInstitutionImage",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the institution the image should be added to.", required = true)
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
fun postUploadImageForInstitution(ctx: Context) {
    /* Obtain parameters. */
    val institutionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400,"Malformed institution ID.")
    val files = ctx.uploadedFiles()
    val delete = mutableListOf<Path>()

    /* Make sure that a file has been uploaded. */
    if (files.isEmpty()) throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Start transaction */
    transaction {
        val institution = Institutions.selectAll().where { Institutions.id eq institutionId }.map { it.toInstitution() }.firstOrNull() ?: throw ErrorStatusException(404, "No Institution with ID $institutionId found.")
        val deployments = ImageDeployments.selectAll().where {
            ImageDeployments.solrInstanceId inSubQuery (InstitutionsSolrCollections innerJoin SolrCollections innerJoin SolrConfigs).select(SolrConfigs.id).where{
                (InstitutionsSolrCollections.institutionId) eq institutionId and (InstitutionsSolrCollections.selected eq true)
            }
        }.map {
            it.toImageDeployment()
        }

        /* Sanity check. */
        if (deployments.isEmpty()) {
            throw ErrorStatusException(400, "No deployment configuration found for institution with ID $institutionId.")
        }

        /* Define file names. */
        val oldFilename = institution.imageName
        val filename = "${institution.id}-${System.currentTimeMillis()}.jpg"

        /* Process images. */
        for (f in ctx.uploadedFiles()) {
            /* Open image. */
            val image = try {
                ImmutableImage.loader().fromStream(f.content())
            } catch (e: IOException) {
                throw ErrorStatusException(400, "Uploaded image file could not be opened due to unhandled exception.")
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
                val path = Paths.get(d.path).resolve("institutions").resolve(d.name).resolve(filename)
                if (oldFilename != null) {
                    delete.add(Paths.get(d.path).resolve("institutions").resolve(d.name).resolve(oldFilename))
                }

                try {
                    /* Prepare deployment path and create directories if necessary. */
                    if (!Files.exists(path.parent)) {
                        Files.createDirectories(path.parent)
                    }

                    /* Write image. */
                    ImageHandler.store(scaled, image.metadata, JpegWriter.Default, path)
                }  catch (e: IOException) {
                    throw ErrorStatusException(500, "Could not deploy image due to unhandled exception.")
                }
            }

            /* Update image name. */
            Institutions.update({ Institutions.id eq institutionId }) { update ->
                update[Institutions.imageName] = filename
                update[modified] = Instant.now()
            }

            /* One image is enough. */
            break
        }
    }

    /* Delete old files. */
    for (oldPath in delete) {
        if (Files.exists(oldPath)) {
            Files.delete(oldPath)
        }
    }
}

@OpenApi(
    path = "/api/institutions/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing institution.",
    operationId = "deleteInstitution",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", type = Int::class, description = "The ID of the institution that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteInstitution(ctx: Context) {
    val institutionId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400,"Malformed institution ID.")
    val deleted = transaction {
        Institutions.deleteWhere { Institutions.id eq institutionId }
    }
    if (deleted > 0) {
        ctx.json(SuccessStatus("Institution with ID $institutionId deleted successfully."))
    } else {
        ctx.json(ErrorStatus(404, "Institution with ID $institutionId could not be deleted because it doesn't exist."))
    }
}



