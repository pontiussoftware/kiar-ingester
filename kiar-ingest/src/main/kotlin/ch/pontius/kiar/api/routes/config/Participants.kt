package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institutions.Participants
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


@OpenApi(
    path = "/api/participants",
    methods = [HttpMethod.GET],
    summary = "Lists all available participants.",
    operationId = "getListParticipants",
    tags = ["Config", "Participant"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun listParticipants(ctx: Context) {
    transaction {
        ctx.json(Participants.select(Participants.name).map { it[Participants.name] }.toTypedArray())
    }
}

@OpenApi(
    path = "/api/participants/{name}",
    methods = [HttpMethod.POST],
    summary = "Creates a new participant.",
    operationId = "postCreateParticipant",
    tags = ["Config", "Participant"],
    pathParams = [
        OpenApiParam("name", String::class, description = "The name of the new participant. Must be unique!", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun createParticipants(ctx: Context) {
    val participantName = ctx.pathParam("name")
    transaction {
        Participants.insert {
            it[name] = Participants.name
        }
    }
    ctx.json(SuccessStatus("Participant '$participantName' created successfully."))
}

@OpenApi(
    path = "/api/participants/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes and existing participant.",
    operationId = "deleteParticipant",
    tags = ["Config", "Participant"],
    pathParams = [
        OpenApiParam("id", Int::class, description = "The ID of the participant to delete.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun deleteParticipants(ctx: Context) {
    val participantId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed participant ID.")
    val deleted = transaction {
        Participants.deleteWhere { Participants.id eq participantId }
    }
    if (deleted > 0) {
        ctx.json(SuccessStatus("Participant with ID $participantId deleted successfully."))
    } else {
        ctx.json(ErrorStatus(404, "Participant with ID could not be found."))
    }
}