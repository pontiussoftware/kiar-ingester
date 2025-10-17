package ch.pontius.kiar.api.routes.session

import ch.pontius.kiar.api.model.session.LoginRequest
import ch.pontius.kiar.api.model.session.SessionStatus
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.institutions.Users.toUser
import ch.pontius.kiar.utilities.extensions.SALT
import ch.pontius.kiar.utilities.extensions.SESSION_USER_ID
import ch.pontius.kiar.utilities.extensions.SESSION_USER_NAME
import ch.pontius.kiar.utilities.extensions.currentUser
import ch.pontius.kiar.utilities.extensions.invalidateUser
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import ch.pontius.kiar.utilities.extensions.setUser
import ch.pontius.kiar.utilities.extensions.validateEmail
import ch.pontius.kiar.utilities.extensions.validatePassword
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant

@OpenApi(
    path = "/api/session/login",
    methods = [HttpMethod.POST],
    summary = "Attempts a login using the credentials provided in the request body.",
    operationId = "login",
    tags = ["Session"],
    requestBody = OpenApiRequestBody([OpenApiContent(LoginRequest::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun login(ctx: Context) {
    val request = ctx.parseBodyOrThrow<LoginRequest>()

    /* Check if user is already logged-in.*/
    if (ctx.sessionAttribute<String>(SESSION_USER_ID) != null && ctx.sessionAttribute<String>(SESSION_USER_NAME) == request.username) {
        ctx.json(SuccessStatus("Already logged in."))
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
        ctx.setUser(user)
        ctx.json(SuccessStatus("Login successful!"))
    }
}

@OpenApi(
    path = "/api/session/logout",
    methods = [HttpMethod.GET],
    summary = "Performs a logout for the currently logged-in user.",
    operationId = "logout",
    tags = ["Session"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)])
    ]
)
fun logout(ctx: Context) {
    ctx.invalidateUser()
    ctx.json(SuccessStatus("Logout successful!"))
}

@OpenApi(
    path = "/api/session/status",
    methods = [HttpMethod.GET],
    summary = "Checks and returns the status of the current session.",
    operationId = "status",
    tags = ["Session"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SessionStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun status(ctx: Context) {
    transaction {
        val user = ctx.currentUser()
        ctx.json(SessionStatus(user.username, user.role))
    }
}


@OpenApi(
    path = "/api/session/user",
    methods = [HttpMethod.GET],
    summary = "Returns information about the currently logged-in user.",
    operationId = "getUser",
    tags = ["Session"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(User::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getUser(ctx: Context) {
    transaction {
        val user = ctx.currentUser()
        ctx.json(user)
    }
}

@OpenApi(
    path = "/api/session/user",
    methods = [HttpMethod.PUT],
    summary = "Updates the currently active user.",
    operationId = "putUpdateCurrentUser",
    tags = ["Session"],
    requestBody = OpenApiRequestBody([OpenApiContent(User::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SessionStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateUser(ctx: Context) {
    val request = ctx.parseBodyOrThrow<User>()

    transaction {
        val user = ctx.currentUser()
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

    ctx.json(SuccessStatus("User ${request.username} updated successfully."))
}
