package ch.pontius.kiar.api.routes.masterdata

import ch.pontius.kiar.api.model.config.image.ImageFormat
import ch.pontius.kiar.api.model.config.mappings.MappingFormat
import ch.pontius.kiar.api.model.config.mappings.ValueParser
import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.api.model.config.transformers.TransformerType
import ch.pontius.kiar.api.model.masterdata.Canton
import ch.pontius.kiar.api.model.masterdata.RightStatement
import io.ktor.server.application.ApplicationCall
import ch.pontius.kiar.api.openapi.*
import io.ktor.server.response.respond

val listRightStatementsDoc: RouteDoc = {
    operationId = "getListRightStatements"
    summary = "Lists all available right statements."
    tags("Masterdata")
    responses {
        json<List<RightStatement>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listRightStatements(call: ApplicationCall) = call.respond(RightStatement.DEFAULT.toList())

val listCantonsDoc: RouteDoc = {
    operationId = "getListCantons"
    summary = "Lists all available cantons."
    tags("Masterdata")
    responses {
        json<List<Canton>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listCantons(call: ApplicationCall) = call.respond(Canton.entries.toList())

val listTransformerTypesDoc: RouteDoc = {
    operationId = "getListTransformerTypes"
    summary = "Lists all available transformer types."
    tags("Config", "Masterdata")
    responses {
        json<List<TransformerType>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listTransformerTypes(call: ApplicationCall) {
    call.respond(TransformerType.entries.filter { it.name != "IMAGE" })
}

val listImageFormatsDoc: RouteDoc = {
    operationId = "getListImageFormats"
    summary = "Lists all available formats available for image deployment."
    tags("Config",  "Apache Solr", "Masterdata")
    responses {
        json<List<ImageFormat>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listImageFormats(call: ApplicationCall) {
    call.respond(ImageFormat.entries.toList())
}

val listMappingFormatsDoc: RouteDoc = {
    operationId = "getListMappingFormats"
    summary = "Lists all available entity mapping formats."
    tags("Config", "Entity Mapping", "Masterdata")
    responses {
        json<List<MappingFormat>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listMappingFormats(call: ApplicationCall) {
    call.respond(MappingFormat.entries.toList())
}

val listParsersDoc: RouteDoc = {
    operationId = "getListParsers"
    summary = "Lists all available parses available for entity mapping."
    tags("Config", "Entity Mapping", "Masterdata")
    responses {
        json<List<ValueParser>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listParsers(call: ApplicationCall) {
    call.respond(ValueParser.entries.toList())
}

val listJobTemplateTypesDoc: RouteDoc = {
    operationId = "getListJobTemplateTypes"
    summary = "Lists all available job template types."
    tags("Config", "Job Template", "Masterdata")
    responses {
        json<List<JobType>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listJobTemplateTypes(call: ApplicationCall) {
    call.respond(JobType.entries.toList())
}