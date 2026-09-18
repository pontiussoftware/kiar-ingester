package ch.pontius.kiar.api.routes.config

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institutions.Participants
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import io.ktor.server.application.ApplicationCall
import ch.pontius.kiar.api.openapi.*
import io.ktor.server.response.respond
import ch.pontius.kiar.utilities.extensions.pathParam


val listParticipantsDoc: RouteDoc = {
    operationId = "getListParticipants"
    summary = "Lists all available participants."
    tags("Config", "Participant")
    responses {
        json<List<String>>(200)
        errors(401, 403, 500)
    }
}

suspend fun listParticipants(call: ApplicationCall) {
    val participants = transaction {
        Participants.select(Participants.name).map { it[Participants.name] }
    }
    call.respond(participants)
}

val createParticipantsDoc: RouteDoc = {
    operationId = "postCreateParticipant"
    summary = "Creates a new participant."
    tags("Config", "Participant")
    parameters {
        pathParam("name", "The name of the new participant. Must be unique!", STRING)
    }
    responses {
        json<SuccessStatus>(200)
        errors(400, 500)
    }
}

suspend fun createParticipants(call: ApplicationCall) {
    val participantName = call.pathParam("name")
    transaction {
        Participants.insert {
            it[name] = participantName
        }
    }
    call.respond(SuccessStatus("Participant '$participantName' created successfully."))
}

val deleteParticipantsDoc: RouteDoc = {
    operationId = "deleteParticipant"
    summary = "Deletes and existing participant."
    tags("Config", "Participant")
    parameters {
        pathParam("id", "The ID of the participant to delete.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(404, 500)
    }
}

suspend fun deleteParticipants(call: ApplicationCall) {
    val participantId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Malformed participant ID.")
    val deleted = transaction {
        Participants.deleteWhere { Participants.id eq participantId }
    }
    if (deleted > 0) {
        call.respond(SuccessStatus("Participant with ID $participantId deleted successfully."))
    } else {
        call.respond(ErrorStatus(404, "Participant with ID could not be found."))
    }
}