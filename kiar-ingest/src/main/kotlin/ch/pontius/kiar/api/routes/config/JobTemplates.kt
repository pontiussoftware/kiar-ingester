package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.config.templates.JobTemplate
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

@OpenApi(
    path = "/api/templates",
    methods = [HttpMethod.GET],
    summary = "Attempts a login using the credentials provided in the request body.",
    operationId = "getListJobTemplates",
    tags = ["Job Templates"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<JobTemplate>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listJobTemplates(ctx: Context, store: TransientEntityStore) {
    /* Validate credentials and log-in user. */
    store.transactional (true) {
        val user = ctx.currentUser()
        val templates = if (user.role == DbRole.ADMINISTRATOR) {
            DbJobTemplate.all()
        } else if (user.institution?.participant != null) {
            DbJobTemplate.filter { it.participant eq user.institution?.participant }
        } else {
            DbJobTemplate.emptyQuery()
        }
        ctx.json(templates.mapToArray { it.toApi() })
    }
}