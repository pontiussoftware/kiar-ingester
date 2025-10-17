package ch.pontius.kiar.api.routes.masterdata

import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.config.mappings.MappingFormat
import ch.pontius.kiar.api.model.config.mappings.ValueParser
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.api.model.config.transformers.TransformerType
import ch.pontius.kiar.api.model.masterdata.Canton
import ch.pontius.kiar.api.model.masterdata.RightStatement
import ch.pontius.kiar.api.model.status.ErrorStatus
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse

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
fun listRightStatements(ctx: Context) = ctx.json(RightStatement.DEFAULT)

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
fun listCantons(ctx: Context) = ctx.json(Canton.entries.toTypedArray())

@OpenApi(
    path = "/api/masterdata/transformers",
    methods = [HttpMethod.GET],
    summary = "Lists all available transformer types.",
    operationId = "getListTransformerTypes",
    tags = ["Config", "Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<TransformerType>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listTransformerTypes(ctx: Context) {
    ctx.json(TransformerType.entries.filter { it.name != "IMAGE" }.toTypedArray())
}

@OpenApi(
    path = "/api/masterdata/image-formats",
    methods = [HttpMethod.GET],
    summary = "Lists all available formats available for image deployment.",
    operationId = "getListImageFormats",
    tags = ["Config",  "Apache Solr", "Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ImageFormat>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listImageFormats(ctx: Context) {
    ctx.json(ImageFormat.entries.toTypedArray())
}

@OpenApi(
    path = "/api/masterdata/mapping-formats",
    methods = [HttpMethod.GET],
    summary = "Lists all available entity mapping formats.",
    operationId = "getListMappingFormats",
    tags = ["Config", "Entity Mapping", "Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<MappingFormat>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listMappingFormats(ctx: Context) {
    ctx.json(MappingFormat.entries.toTypedArray())
}

@OpenApi(
    path = "/api/masterdata/parsers",
    methods = [HttpMethod.GET],
    summary = "Lists all available parses available for entity mapping.",
    operationId = "getListParsers",
    tags = ["Config", "Entity Mapping", "Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<ValueParser>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listParsers(ctx: Context) {
    ctx.json(ValueParser.entries.toTypedArray())
}

@OpenApi(
    path = "/api/masterdata/job-types",
    methods = [HttpMethod.GET],
    summary = "Lists all available job template types.",
    operationId = "getListJobTemplateTypes",
    tags = ["Config", "Job Template", "Masterdata"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<JobType>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listJobTemplateTypes(ctx: Context) {
    ctx.json(JobType.entries.toTypedArray())
}