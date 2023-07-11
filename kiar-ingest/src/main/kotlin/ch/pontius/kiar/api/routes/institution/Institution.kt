import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.job.Job
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.sortedBy

@OpenApi(
    path = "/api/institutions",
    methods = [HttpMethod.GET],
    summary = "Retrieves all jobs that are currently active. Non-administrator users can only see Jobs that belong to them.",
    operationId = "getInstitutions",
    tags = ["Institution"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<Institution>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getListInstitutions(ctx: Context, store: TransientEntityStore) {
    val result = store.transactional(true) {
        DbInstitution.all().sortedBy(DbInstitution::name).mapToArray { it.toApi() }
    }
    ctx.json(result)
}