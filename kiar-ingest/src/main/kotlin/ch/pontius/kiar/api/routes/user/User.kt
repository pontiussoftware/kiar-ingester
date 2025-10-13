package ch.pontius.kiar.api.routes.user

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.PaginatedUserResult
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.institutions.Users.toUser
import ch.pontius.kiar.utilities.extensions.SALT
import ch.pontius.kiar.utilities.extensions.parseBodyOrThrow
import ch.pontius.kiar.utilities.extensions.validateEmail
import ch.pontius.kiar.utilities.extensions.validatePassword
import io.javalin.http.Context
import io.javalin.openapi.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant


@OpenApi(
    path = "/api/users",
    methods = [HttpMethod.GET],
    summary = "Retrieves all users registered in the database.",
    operationId = "getUsers",
    tags = ["User"],
    pathParams = [],
    queryParams = [
        OpenApiParam(name = "page", type = Int::class, description = "The page index (zero-based) for pagination.", required = false),
        OpenApiParam(name = "pageSize", type = Int::class, description = "The page size for pagination.", required = false),
        OpenApiParam(name = "order", type = String::class, description = "The attribute to order by. Possible values are 'name', 'email', 'inactive'.", required = false),
        OpenApiParam(name = "orderDir", type = String::class, description = "The sort order. Possible values are 'asc' and 'desc'.", required = false)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(PaginatedUserResult::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getListUsers(ctx: Context) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val order = ctx.queryParam("order")?.lowercase() ?: "name"
    val orderDir = ctx.queryParam("orderDir")?.lowercase() ?: "asc"

    val (total, users) = transaction {
        val total = Users.selectAll().count()
        val order = when (order) {
            "email" -> Users.email
            "inactive" -> Users.inactive
            else -> Users.name
        }
        val users = (Users leftJoin Institutions).selectAll()
            .orderBy(order, SortOrder.valueOf(orderDir.uppercase()) )
            .offset((page * pageSize).toLong())
            .limit(pageSize)
            .map { it.toUser() }
        total to users
    }

    ctx.json(PaginatedUserResult(total, page, pageSize, users))
}

@OpenApi(
    path = "/api/users/roles",
    methods = [HttpMethod.GET],
    summary = "Lists all available roles.",
    operationId = "getListRoles",
    tags = ["User"],
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(Array<Role>::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun getListRoles(ctx: Context) {
    ctx.json(Role.entries.toTypedArray())
}

@OpenApi(
    path = "/api/users",
    methods = [HttpMethod.POST],
    summary = "Creates a new user.",
    operationId = "postCreateUser",
    tags = ["User"],
    requestBody = OpenApiRequestBody([OpenApiContent(User::class)], required = true),
    pathParams = [],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postCreateUser(ctx: Context) {
    val request = ctx.parseBodyOrThrow<User>()

    /* Check if password is present. */
    if (request.password == null) {
        throw ErrorStatusException(400, "Missing password.")
    }

    /* Validate password */
    if (!request.password.validatePassword()) {
        throw ErrorStatusException(400, "Invalid password. Password must consist of printable ASCII characters and have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
    }

    /* Validate e-mail */
    if (request.email != null) {
        if (!request.email.validateEmail()) throw ErrorStatusException(400, "Invalid e-mail address.")
    }

    /* Create new job. */
    val user = transaction {
        val userId = Users.insertAndGetId { user ->
            user[name] = request.username.lowercase()
            user[email] = request.email?.lowercase()
            user[password] = BCrypt.hashpw(request.password, SALT)
            user[inactive] = !request.active
            user[role] = request.role
            user[institutionId] = request.institution?.name?.let { name ->
                Institutions.select(Institutions.id).where { Institutions.name eq name }.map { it[Institutions.id].value }.firstOrNull()
            }
        }.value
        request.copy(id = userId)
    }

    /* Return job object. */
    ctx.json(SuccessStatus("User '${user.username}' (ID: ${user.id}) created successfully."))
}

@OpenApi(
    path = "/api/users/{id}",
    methods = [HttpMethod.PUT],
    summary = "Updates an existing user.",
    operationId = "putUpdateUser",
    tags = ["User"],
    requestBody = OpenApiRequestBody([OpenApiContent(User::class)], required = true),
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the user that should be updated.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun putUpdateUser(ctx: Context) {
    val userId = ctx.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Invalid user ID.")
    val request = ctx.parseBodyOrThrow<User>()

    /* Update user. */
    transaction {
        Users.update({ Users.id eq userId }) { user ->
            user[name] = request.username.lowercase()
            if (request.email != null) {
                if(!request.email.validateEmail()) throw ErrorStatusException(400, "Invalid e-mail address.")
                user[email]  = request.email.lowercase()
            }
            if (request.password != null) {
                if (!request.password.validatePassword()) {
                    throw ErrorStatusException(400, "Invalid password. Password must consist of printable ASCII characters and have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
                }
                user[password] = BCrypt.hashpw(request.password, SALT)
            }
            user[inactive] = !request.active
            user[role] = request.role
            user[institutionId] = request.institution?.name?.let { name ->
                Institutions.select(Institutions.id).where { Institutions.name eq name }.map { it[Institutions.id].value }.firstOrNull()
            }
            user[modified] = Instant.now()
        }
    }

    /* Return job object. */
    ctx.json(SuccessStatus("User '${request.username}' (ID: $userId) updated successfully."))
}

@OpenApi(
    path = "/api/users/{id}",
    methods = [HttpMethod.DELETE],
    summary = "Deletes an existing user.",
    operationId = "deleteUser",
    tags = ["User"],
    pathParams = [
        OpenApiParam(name = "id", description = "The ID of the user that should be deleted.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun deleteUser(ctx: Context) {
    val userId = ctx.pathParam("id").toIntOrNull() ?: throw  ErrorStatusException(400, "Invalid user ID.")
    val count = transaction {
        Users.deleteWhere { Users.id eq userId }
    }
    if (count > 0) {
        ctx.json(SuccessStatus("User  with ID$userId deleted successfully."))
    } else {
        ctx.json(ErrorStatus(404, "User with ID $userId could not be found."))
    }
}