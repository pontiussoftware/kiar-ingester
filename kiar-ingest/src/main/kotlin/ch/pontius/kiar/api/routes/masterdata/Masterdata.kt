package ch.pontius.kiar.api.routes.masterdata

import ch.pontius.kiar.api.model.masterdata.Canton
import ch.pontius.kiar.api.model.masterdata.RightStatement
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.database.masterdata.DbCanton
import ch.pontius.kiar.database.masterdata.DbRightStatement
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore

@OpenApi(
    path = "/api/masterdata/rightstatements",
    methods = [HttpMethod.GET],
    summary = "Lists all available right statements.",
    operationId = "getListRightStatements",
    tags = ["Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<RightStatement>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listRightStatements(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbRightStatement.all().mapToArray { it.toApi() })
    }
}

@OpenApi(
    path = "/api/masterdata/cantons",
    methods = [HttpMethod.GET],
    summary = "Lists all available cantons.",
    operationId = "getListCantons",
    tags = ["Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<Canton>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listCantons(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbCanton.all().mapToArray { it.toApi() })
    }
}