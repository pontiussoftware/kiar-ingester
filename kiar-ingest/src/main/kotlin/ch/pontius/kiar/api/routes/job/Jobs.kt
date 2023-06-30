package ch.pontius.kiar.api.routes.job

import ch.pontius.kiar.api.model.job.Job
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.routes.session.currentUser
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.job.DbJob
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.emptyQuery
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.toList


@OpenApi(
    path = "/api/jobs/active",
    methods = [HttpMethod.GET],
    summary = "Retrieves all jobs that are currently active. Non-administrator users can only see Jobs that belong to them.",
    operationId = "getActiveJobs",
    tags = ["Job"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<Job>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getActiveJobs(ctx: Context, store: TransientEntityStore) {
    store.transactional(true) {
        val currentUser = ctx.currentUser()
        val jobs = when (currentUser.role) {
            DbRole.ADMINISTRATOR -> DbJob.filter { it.status.active eq true }
            DbRole.MANAGER, DbRole.VIEWER -> {
                val participant = currentUser.institution?.participant
                if (participant == null) {
                    DbJob.emptyQuery()
                } else {
                    DbJob.filter { (it.status.active eq true) and (it.template?.participant eq participant) }
                }
            }
            else -> DbJob.emptyQuery()
        }
        ctx.json(jobs.toList())
    }
}

@OpenApi(
    path = "/api/jobs/inactive",
    methods = [HttpMethod.GET],
    summary = "Retrieves all jobs that are currently inactive (job history). Non-administrator users can only see Jobs that belong to them.",
    operationId = "getInactiveJobs",
    tags = ["Job"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<Job>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getInactiveJobs(ctx: Context, store: TransientEntityStore) {
    store.transactional(true) {
        val currentUser = ctx.currentUser()
        val jobs = when (currentUser.role) {
            DbRole.ADMINISTRATOR -> DbJob.filter { it.status.active eq false }
            DbRole.MANAGER, DbRole.VIEWER -> {
                val participant = currentUser.institution?.participant
                if (participant == null) {
                    DbJob.emptyQuery()
                } else {
                    DbJob.filter { (it.status.active eq false) and (it.template?.participant eq participant) }
                }
            }
            else -> DbJob.emptyQuery()
        }
        ctx.json(jobs.toList())
    }
}