import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.institution.PaginatedInstitutionResult
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.masterdata.DbRightStatement
import ch.pontius.kiar.utilities.Geocoding
import ch.pontius.kiar.utilities.mapToArray
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import org.joda.time.DateTime
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

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
fun getListInstitutions(ctx: Context, store: TransientEntityStore) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val order = ctx.queryParam("order")?.lowercase() ?: "name"
    val orderDir = ctx.queryParam("orderDir")?.lowercase() ?: "asc"
    val filter = ctx.queryParam("filter")
    val (total, results) = store.transactional(true) {
        /* Prepare query based on presence of filter. */
        var query = if (filter == null) {
            DbInstitution.all()
        } else {
            DbInstitution.filter { ((it.name startsWith filter) or (it.displayName startsWith filter) or (it.city startsWith filter)) }
        }

        /* Parse sort order. */
        query = when(order) {
            "city" -> query.sortedBy(DbInstitution::city, orderDir == "asc")
            "zip" -> query.sortedBy(DbInstitution::zip, orderDir == "asc")
            "canton" -> query.sortedBy(DbInstitution::canton, orderDir == "asc")
            "publish" -> query.sortedBy(DbInstitution::publish, orderDir == "asc")
            else -> query.sortedBy(DbInstitution::name, orderDir == "asc")
        }

        /* Execute query and return paginated result. */
        query.size() to query.drop(page * pageSize).take(pageSize).asSequence().map { it.toApi() }.toList()
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
fun getListInstitutionNames(ctx: Context, store: TransientEntityStore) {
    val list = store.transactional(true) {
        DbInstitution.all().sortedBy(DbInstitution::name).mapToArray { it.name }
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
fun postCreateInstitution(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(Institution::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Create new job. */
    val institution = store.transactional {
        /* Create new job. */
        DbInstitution.new {
            this.name = request.name
            this.displayName = request.displayName
            this.description = request.description
            this.isil = request.isil
            this.street = request.street
            this.city = request.city
            this.zip = request.zip
            this.canton = request.canton
            this.publish = request.publish
            this.email = request.email
            this.homepage = request.homepage
            this.participant = DbParticipant.filter { it.name eq request.participantName }.firstOrNull()
                ?: throw ErrorStatusException(404, "Participant ${request.participantName} could not be found.")
            this.createdAt = DateTime.now()
            this.changedAt = DateTime.now()

            /* Applies longitude and latitude, depending on what is available. */
            if (request.longitude == null || request.latitude == null) {
                val geocoding = Geocoding.geocode(request.street, request.city, request.zip)
                if (geocoding != null) {
                    this.longitude = geocoding.lon
                    this.latitude = geocoding.lat
                }
            } else {
                this.longitude = request.longitude!!
                this.latitude = request.latitude!!
            }
        }.toApi()
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Institution '${institution.name}' (id: ${institution.id}) created successfully."))
}
@OpenApi(
    path = "/api/institutions/{id}",
    methods = [HttpMethod.GET],
    summary = "Gets information about an existing institution.",
    operationId = "getInstitution",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the institution that should be fetched.", required = true)
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

fun getInstitution(ctx: Context, store: TransientEntityStore) {
    val institutionId = ctx.pathParam("id")

    /* Create new job. */
    val institution = store.transactional(true) {
        try {
            DbInstitution.findById(institutionId).toApi()
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }
    }

    /* Return job object. */
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
        OpenApiParam(name = "id", description = "The ID of the institution that should be updated.", required = true)
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
fun putUpdateInstitution(ctx: Context, store: TransientEntityStore) {
    val institutionId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(Institution::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Create new job. */
    val institutionName = store.transactional {
        val institution = try {
            DbInstitution.findById(institutionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }

        /* Make sure, that the current user can actually edit this institution. */
        val currentUser = ctx.currentUser()
        if (currentUser.role != DbRole.ADMINISTRATOR && currentUser.institution != institution) {
            throw ErrorStatusException(403, "Institution with ID $institutionId cannot be edited by current user.")
        }

        /* Update institution. */
        institution.displayName = request.displayName
        institution.description = request.description
        institution.isil = request.isil
        institution.street = request.street
        institution.city = request.city
        institution.zip = request.zip
        institution.email = request.email
        institution.homepage = request.homepage
        institution.defaultCopyright = request.defaultCopyright
        institution.defaultRightStatement = DbRightStatement.filter { it.short eq request.defaultRightStatement }.singleOrNull()
        institution.defaultObjectUrl = request.defaultObjectUrl
        institution.changedAt = DateTime.now()

        /* Applies longitude and latitude, depending on what is available. */
        if (request.longitude == null || request.latitude == null) {
            val geocoding = Geocoding.geocode(request.street, request.city, request.zip)
            if (geocoding != null) {
                institution.longitude = geocoding.lon
                institution.latitude = geocoding.lat
            }
        } else {
            institution.longitude = request.longitude!!
            institution.latitude = request.latitude!!
        }

        /* Some data can only be edited by an administrator. */
        if (currentUser.role == DbRole.ADMINISTRATOR) {
            institution.name = request.name
            institution.participant = DbParticipant.filter { it.name eq request.participantName }.firstOrNull() ?: throw ErrorStatusException(404, "Participant ${request.participantName} could not be found.")
            institution.canton = request.canton
            institution.publish = request.publish

            /* Clear and reconnect available collections. */
            institution.availableCollections.clear()
            for (collection in DbCollection.filter { (it.name isIn request.availableCollections) and (it.type.description eq DbCollectionType.OBJECT.description )}.asSequence()) {
                institution.availableCollections.add(collection)
            }
        }

        /* Clear and reconnect selected collections. */
        institution.selectedCollections.clear()
        for (collection in institution.availableCollections.filter { c -> c.name isIn request.selectedCollections }.asSequence()) {
            institution.selectedCollections.add(collection)
        }

        institution.name
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Institution '$institutionName' (id: $institutionId) updated successfully."))
}
@OpenApi(
    path = "/api/institutions/{id}/image",
    methods = [HttpMethod.GET],
    summary = "Gets the preview image for the provided institution.",
    operationId = "getInstitutionImage",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the institution the image should be retrieved for.", required = true)
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
fun getImageForInstitution(ctx: Context, store: TransientEntityStore) {
    /* Obtain parameters. */
    val institutionId = ctx.pathParam("id")

    /* Start transaction */
    store.transactional(true) {
        val institution = try {
            DbInstitution.findById(institutionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }

        /* Obtain image name. */
        val imageName = institution.imageName ?: throw ErrorStatusException(404, "No image found for institution with ID $institutionId.")

        /* Obtain deployment path. */
        val deployment = institution.availableCollections.mapDistinct { it.solr }.flatMapDistinct { it.deployments }.asSequence().map { it.toApi() }.firstOrNull() ?: throw ErrorStatusException(404, "No deployment found for institution with ID $institutionId.")

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
}

@OpenApi(
    path = "/api/institutions/{id}/image",
    methods = [HttpMethod.POST],
    summary = "Posts a new image for the provided institution.",
    operationId = "postInstitutionImage",
    tags = ["Institution"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the institution the image should be added to.", required = true)
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
fun postUploadImageForInstitution(ctx: Context, store: TransientEntityStore) {
    /* Obtain parameters. */
    val institutionId = ctx.pathParam("id")
    val files = ctx.uploadedFiles()
    val delete = mutableListOf<Path>()

    /* Make sure that a file has been uploaded. */
    if (files.isEmpty()) throw ErrorStatusException(401, "Uploaded file is missing.")

    /* Start transaction */
    store.transactional {
        val institution = try {
            DbInstitution.findById(institutionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }

        /* List of deployment paths. */
        val deployments = institution.availableCollections.mapDistinct {
            it.solr
        }.flatMapDistinct {
            it.deployments
        }.asSequence().map { it.toApi() }.toList()
        if (deployments.isEmpty()) {
            throw ErrorStatusException(400, "No deployment configuration found for institution-")
        }

        /* Define file names. */
        val oldFilename = institution.imageName
        val filename = "${institution.xdId}-${System.currentTimeMillis()}.jpg"

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
                    scaled.output(JpegWriter.Default, path)
                }  catch (e: IOException) {
                    throw ErrorStatusException(400, "Could not deploy image.")
                }
            }

            /* Update image name. */
            institution.imageName = filename

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
        OpenApiParam(name = "id", description = "The ID of the institution that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteInstitution(ctx: Context, store: TransientEntityStore) {
    val institutionId = ctx.pathParam("id")
    val institutionName = store.transactional {
        val institution = try {
            DbInstitution.findById(institutionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }
        val name = institution.name
        institution.delete()
        name
    }
    ctx.json(SuccessStatus("Institution '$institutionName' (id: $institutionId) deleted successfully."))
}



