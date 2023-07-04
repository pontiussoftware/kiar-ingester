package ch.pontius.kiar.api.routes.session

import ch.pontius.kiar.api.model.session.LoginRequest
import ch.pontius.kiar.api.model.session.SessionStatus
import ch.pontius.kiar.api.model.session.User
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.utilities.validateEmail
import ch.pontius.kiar.utilities.validatePassword
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.kotlin.notNull
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.util.findById
import org.mindrot.jbcrypt.BCrypt

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
fun login(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(LoginRequest::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed login request.")
    }

    /* Check if user is already logged-in.*/
    if (ctx.sessionAttribute<String>(SESSION_USER_ID) != null && ctx.sessionAttribute<String>(SESSION_USER_NAME) == request.username) {
        ctx.json(SuccessStatus("Already logged in."));
        return
    }

    /* Validate credentials and log-in user. */
    store.transactional (true) {
        val user = DbUser.filter { (it.name eq request.username) and (it.inactive eq false) }.firstOrNull()
            ?: throw ErrorStatusException(401, "The provided credentials are invalid.")

        if (!BCrypt.checkpw(request.password, user.password)) {
            throw ErrorStatusException(401, "The provided credentials are invalid.")
        } else {
            ctx.setUser(user)
            ctx.json(SuccessStatus("Login successful!"))
        }
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
fun status(ctx: Context, store: TransientEntityStore) {
    store.transactional(true) {
        val user = ctx.currentUser()
        ctx.json(SessionStatus(user.name, user.role.toApi()))
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
fun getUser(ctx: Context, store: TransientEntityStore) {
    store.transactional(true) {
        val user = ctx.currentUser()
        ctx.json(user.toApi())
    }
}

@OpenApi(
    path = "/api/session/user",
    methods = [HttpMethod.PUT],
    summary = "Updates the currently active user.",
    operationId = "putUpdateUser",
    tags = ["Session"],
    requestBody = OpenApiRequestBody([OpenApiContent(User::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SessionStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun updateUser(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(User::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed login request.")
    }

    store.transactional {
        val user = ctx.currentUser()

        if (user.xdId != request.id || user.name != request.username) {
            throw ErrorStatusException(400, "Provided user does not correspond with currently logged in user.")
        }

        /* Update user password. */
        if (request.password != null) {
            if(!request.password.validatePassword()) throw ErrorStatusException(400, "Invalid password. Password must have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
            user.password = BCrypt.hashpw(request.password, SALT)
        }

        /* Update user e-mail. */
        if (request.email != null) {
            if(!request.email.validateEmail()) throw ErrorStatusException(400, "Invalid e-mail address.")
            user.email = request.email
        }
    }

    ctx.json(SuccessStatus("User ${request.username} updated successfully."))
}
