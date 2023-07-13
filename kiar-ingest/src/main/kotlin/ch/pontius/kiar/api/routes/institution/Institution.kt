import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.institution.PaginatedInstitutionResult
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.config.solr.DbCollectionType
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import org.apache.solr.client.solrj.impl.Http2SolrClient

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
        OpenApiParam(name = "orderDir", type = String::class, description = "The sort order. Possible values are 'asc' and 'desc'.", required = false)
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
    val result = store.transactional(true) {
        val institutions = when(order) {
            "city" -> DbInstitution.all().sortedBy(DbInstitution::city, orderDir == "asc")
            "zip" -> DbInstitution.all().sortedBy(DbInstitution::zip, orderDir == "asc")
            "canton" -> DbInstitution.all().sortedBy(DbInstitution::canton, orderDir == "asc")
            "publish" -> DbInstitution.all().sortedBy(DbInstitution::publish, orderDir == "asc")
            else -> DbInstitution.all().sortedBy(DbInstitution::name, orderDir == "asc")
        }.drop(page * pageSize).take(pageSize).mapToArray { it.toApi() }
        val total = DbInstitution.all().size()
        total to institutions

    }
    ctx.json(PaginatedInstitutionResult(result.first, page, pageSize, result.second))
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
        OpenApiResponse("200", [OpenApiContent(Institution::class)]),
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
        }.toApi()
    }

    /* Return job object. */
    ctx.json(institution)
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


