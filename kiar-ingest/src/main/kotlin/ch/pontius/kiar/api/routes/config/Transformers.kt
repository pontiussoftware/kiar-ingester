package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.config.transformers.TransformerType
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.database.config.transformers.DbTransformerType
import ch.pontius.kiar.utilities.mapToArray
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter

@OpenApi(
    path = "/api/transformers/types",
    methods = [HttpMethod.GET],
    summary = "Lists all available transformer types.",
    operationId = "getListTransformerTypes",
    tags = ["Config", "Transformer"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<TransformerType>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listTransformerTypes(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        val mappings = DbTransformerType.filter { it.name ne "IMAGE" }
        ctx.json(mappings.mapToArray { it.toApi() })
    }
}

