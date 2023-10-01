package ch.pontius.kiar.api.routes.user

import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.PaginatedUserResult
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.routes.session.SALT
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.utilities.mapToArray
import ch.pontius.kiar.utilities.validatePassword
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt


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
fun getListUsers(ctx: Context, store: TransientEntityStore) {
    val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
    val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 50
    val order = ctx.queryParam("order")?.lowercase() ?: "name"
    val orderDir = ctx.queryParam("orderDir")?.lowercase() ?: "asc"
    val result = store.transactional(true) {
        val users = when(order) {
            "email" -> DbUser.all().sortedBy(DbUser::email, orderDir == "asc")
            "inactive" -> DbUser.all().sortedBy(DbUser::inactive, orderDir == "asc")
            else -> DbUser.all().sortedBy(DbUser::name, orderDir == "asc")
        }.drop(page * pageSize).take(pageSize).mapToArray { it.toApi() }
        val total = DbUser.all().size()
        total to users

    }
    ctx.json(PaginatedUserResult(result.first, page, pageSize, result.second))
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
fun getListRoles(ctx: Context, store: TransientEntityStore) {
    store.transactional (true) {
        ctx.json(DbRole.all().mapToArray { it.toApi() })
    }
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
fun postCreateUser(ctx: Context, store: TransientEntityStore) {
    val request = try {
        ctx.bodyAsClass(User::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Check if password is present. */
    if (request.password == null) {
        throw ErrorStatusException(400, "Missing password.")
    }

    /* Validate password */
    if (!request.password.validatePassword()) {
        throw ErrorStatusException(400, "Invalid password. Password must consist of printable ASCII characters and have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
    }

    /* Create new job. */
    val user = store.transactional {
        /* Create new job. */
        DbUser.new {
            this.name = request.username.lowercase()
            this.password = BCrypt.hashpw(request.password, SALT)
            this.inactive = !request.active
            this.role = request.role.toDb()
            this.institution = request.institution?.let { name -> DbInstitution.filter { it.name eq name }.singleOrNull() ?: throw ErrorStatusException(400, "Specified institution '$name' does not exist.")  }
            this.createdAt = DateTime.now()
            this.changedAt = DateTime.now()
        }.toApi()
    }

    /* Return job object. */
    ctx.json(SuccessStatus("User '${user.username}' (id: ${user.id}) created successfully."))
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
fun putUpdateUser(ctx: Context, store: TransientEntityStore) {
    val institutionId = ctx.pathParam("id")
    val request = try {
        ctx.bodyAsClass(User::class.java)
    } catch (e: BadRequestResponse) {
        throw ErrorStatusException(400, "Malformed request.")
    }

    /* Create new job. */
    val userName = store.transactional {
        val user = try {
            DbUser.findById(institutionId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "Institution with ID $institutionId could not be found.")
        }

        /* Validate and update password (if present). */
        if (request.password != null) {
            if (!request.password.validatePassword()) {
                throw ErrorStatusException(400, "Invalid password. Password must consist of printable ASCII characters and have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
            }
            user.password = BCrypt.hashpw(request.password, SALT)
        }

        /* Update user. */
        user.name = request.username
        user.inactive = !request.active
        user.institution = request.institution?.let { name -> DbInstitution.filter { it.name eq name }.singleOrNull() ?: throw ErrorStatusException(400, "Specified institution '$name' does not exist.")  }
        user.role = request.role.toDb()
        user.changedAt = DateTime.now()
    }

    /* Return job object. */
    ctx.json(SuccessStatus("Institution '$userName' (id: $institutionId) updated successfully."))
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
fun deleteUser(ctx: Context, store: TransientEntityStore) {
    val userId = ctx.pathParam("id")
    val userName = store.transactional {
        val user = try {
            DbUser.findById(userId)
        } catch (e: Throwable) {
            throw ErrorStatusException(404, "User with ID $userId could not be found.")
        }
        val name = user.name
        user.delete()
        name
    }
    ctx.json(SuccessStatus("User '$userName' (ID: $userId) deleted successfully."))
}