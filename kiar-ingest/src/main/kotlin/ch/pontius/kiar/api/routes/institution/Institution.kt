import ch.pontius.kiar.api.model.institution.PaginatedInstitutionResult
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

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

