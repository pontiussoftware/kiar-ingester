package ch.pontius.kiar.api.routes.session

import ch.pontius.kiar.api.model.session.LoginRequest
import ch.pontius.kiar.api.model.session.SessionStatus
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.database.institution.DbUser
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
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
