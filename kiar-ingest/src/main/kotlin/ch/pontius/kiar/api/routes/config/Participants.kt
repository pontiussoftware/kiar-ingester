package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institution.DbParticipant
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.util.findById


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
fun listParticipants(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbParticipant.all().asSequence().map { it.name }.toList().toTypedArray())
    }
}

@OpenApi(
    path = "/api/participants/{name}",
    methods = [HttpMethod.POST],
    summary = "Creates a new participant.",
    operationId = "getListParticipants",
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
fun createParticipants(ctx: Context, store: TransientEntityStore) {
    val participantName = ctx.pathParam("name")
    store.transactional {
        DbParticipant.new {
            name = participantName
        }
    }
    ctx.json(SuccessStatus("Participant '$participantName' created successfully."))
}

@OpenApi(
    path = "/api/participants/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes and existing participant.",
    operationId = "getListParticipants",
    tags = ["Config", "Participant"],
    pathParams = [
        OpenApiParam("id", String::class, description = "The ID of the participant to delete.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun deleteParticipants(ctx: Context, store: TransientEntityStore) {
    val participantId = ctx.pathParam("id")
    store.transactional {
        val participant = try {
            DbParticipant.findById(participantId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Participant with ID $participantId could not be found.")
        }
        participant.delete()
    }
    ctx.json(SuccessStatus("Participant $participantId deleted successfully."))
}