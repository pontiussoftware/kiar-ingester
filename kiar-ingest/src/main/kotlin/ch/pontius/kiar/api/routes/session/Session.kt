package ch.pontius.kiar.api.routes.session

import ch.pontius.kiar.api.model.session.LoginRequest
import ch.pontius.kiar.api.model.session.SessionStatus
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.institutions.Users.toUser
import ch.pontius.kiar.utilities.extensions.SALT
import ch.pontius.kiar.api.UserSession
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.extensions.invalidateUser
import ch.pontius.kiar.utilities.extensions.receiveOrThrow
import ch.pontius.kiar.utilities.extensions.setUser
import ch.pontius.kiar.utilities.extensions.validateEmail
import ch.pontius.kiar.utilities.extensions.validatePassword
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import io.ktor.server.application.ApplicationCall
import ch.pontius.kiar.api.openapi.*
import io.ktor.server.response.respond

val loginDoc: RouteDoc = {
    operationId = "login"
    summary = "Attempts a login using the credentials provided in the request body."
    tags("Session")
    jsonBody<LoginRequest>()
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 500)
    }
}

suspend fun login(call: ApplicationCall) {
    val request = call.receiveOrThrow<LoginRequest>()

    /* Check if user is already logged-in.*/
    val session = call.sessions.get<UserSession>()
    if (session != null && session.username == request.username) {
        call.respond(SuccessStatus("Already logged in."))
        return
    }

    /* Find active user with given username. */
    val user = transaction {
        Users.selectAll().where {
            Users.name eq request.username and (Users.inactive eq false)
        }.map { it.toUser() }.firstOrNull()
    } ?: throw ErrorStatusException(401, "The provided credentials are invalid.")

    /* Check password. */
    if (!BCrypt.checkpw(request.password, user.password)) {
        throw ErrorStatusException(401, "The provided credentials are invalid.")
    } else {
        call.setUser(user)
        call.respond(SuccessStatus("Login successful!"))
    }
}

val logoutDoc: RouteDoc = {
    operationId = "logout"
    summary = "Performs a logout for the currently logged-in user."
    tags("Session")
    responses {
        json<SuccessStatus>(200)
    }
}

suspend fun logout(call: ApplicationCall) {
    call.invalidateUser()
    call.respond(SuccessStatus("Logout successful!"))
}

val statusDoc: RouteDoc = {
    operationId = "status"
    summary = "Checks and returns the status of the current session."
    tags("Session")
    responses {
        json<SessionStatus>(200)
        errors(403)
    }
}

suspend fun status(call: ApplicationCall) {
    val user = transaction {
        call.currentUser()
    }
    call.respond(SessionStatus(user.username, user.role))
}


val getUserDoc: RouteDoc = {
    operationId = "getUser"
    summary = "Returns information about the currently logged-in user."
    tags("Session")
    responses {
        json<User>(200)
        errors(403)
    }
}

suspend fun getUser(call: ApplicationCall) {
    val user = transaction {
        call.currentUser()
    }
    call.respond(user)
}

val updateUserDoc: RouteDoc = {
    operationId = "putUpdateCurrentUser"
    summary = "Updates the currently active user."
    tags("Session")
    jsonBody<User>()
    responses {
        json<SessionStatus>(200)
        errors(400, 403)
    }
}

suspend fun updateUser(call: ApplicationCall) {
    val request = call.receiveOrThrow<User>()

    transaction {
        val user = call.currentUser()
        if (user.id != request.id || user.username != request.username) {
            throw ErrorStatusException(400, "Provided user does not correspond with currently logged in user.")
        }

        /* Update users object. */
        Users.update( { Users.id eq request.id!! }) {
            if (request.password != null) {
                if(!request.password.validatePassword()) throw ErrorStatusException(400, "Invalid password. Password must have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
                it[Users.password] = BCrypt.hashpw(request.password, SALT)
            }

            if (request.email != null) {
                if(!request.email.validateEmail()) throw ErrorStatusException(400, "Invalid e-mail address.")
                it[Users.email] =request.email.lowercase()
            }

            it[Users.modified] = Instant.now()
        }
    }

    call.respond(SuccessStatus("User ${request.username} updated successfully."))
}
