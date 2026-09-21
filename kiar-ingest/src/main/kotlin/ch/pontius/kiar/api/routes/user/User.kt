package ch.pontius.kiar.api.routes.user

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.user.PaginatedUserResult
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.database.institutions.Institutions
import ch.pontius.kiar.database.institutions.Participants
import ch.pontius.kiar.database.institutions.Users
import ch.pontius.kiar.database.institutions.Users.toUser
import ch.pontius.kiar.utilities.extensions.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant


val getListUsersDoc: RouteDoc = {
    operationId = "getUsers"
    summary = "Retrieves all users registered in the database."
    tags("User")
    parameters {
        queryParam("page", "The page index (zero-based) for pagination.", INT32)
        queryParam("pageSize", "The page size for pagination.", INT32)
        queryParam("order", "The attribute to order by. Possible values are 'name', 'email', 'inactive'.")
        queryParam("orderDir", "The sort order. Possible values are 'asc' and 'desc'.")
    }
    responses {
        json<PaginatedUserResult>(200)
        errors(401, 403, 500)
    }
}

suspend fun getListUsers(call: ApplicationCall) {
    val (page, pageSize) = call.pagination()
    val order = call.queryParam("order")?.lowercase() ?: "name"
    val orderDir = call.queryParam("orderDir")?.uppercase()?.let {
        try {
            SortOrder.valueOf(it)
        } catch (_: Throwable) {
            null
        }
    } ?: SortOrder.ASC

    val (total, users) = transaction {
        val total = Users.selectAll().count()
        val order = when (order) {
            "email" -> Users.email
            "inactive" -> Users.inactive
            else -> Users.name
        }
        val users = (Users leftJoin Institutions leftJoin Participants).selectAll()
            .orderBy(order, orderDir)
            .offset(page.toLong() * pageSize)
            .limit(pageSize)
            .map { it.toUser() }
        total to users
    }

    call.respond(PaginatedUserResult(total, page, pageSize, users))
}

val getListRolesDoc: RouteDoc = {
    operationId = "getListRoles"
    summary = "Lists all available roles."
    tags("User")
    responses {
        json<List<Role>>(200)
        errors(401, 403, 500)
    }
}

suspend fun getListRoles(call: ApplicationCall) {
    call.respond(Role.entries.toList())
}

val postCreateUserDoc: RouteDoc = {
    operationId = "postCreateUser"
    summary = "Creates a new user."
    tags("User")
    jsonBody<User>()
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun postCreateUser(call: ApplicationCall) {
    val request = call.receiveOrThrow<User>()

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
            user[password] = BCrypt.hashpw(request.password, BCrypt.gensalt(BCRYPT_COST))
            user[inactive] = !request.active
            user[role] = request.role
            user[institutionId] = request.institution?.name?.let { name ->
                Institutions.select(Institutions.id).where { Institutions.name eq name }.map { it[Institutions.id].value }.firstOrNull()
            }
        }.value
        request.copy(id = userId)
    }

    /* Return job object. */
    call.respond(SuccessStatus("User '${user.username}' (ID: ${user.id}) created successfully."))
}

val putUpdateUserDoc: RouteDoc = {
    operationId = "putUpdateUser"
    summary = "Updates an existing user."
    tags("User")
    parameters {
        pathParam("id", "The ID of the user that should be updated.")
    }
    jsonBody<User>()
    responses {
        json<SuccessStatus>(200)
        errors(400, 401, 403, 404, 500)
    }
}

suspend fun putUpdateUser(call: ApplicationCall) {
    val userId = call.pathParam("id").toIntOrNull() ?: throw ErrorStatusException(400, "Invalid user ID.")
    val request = call.receiveOrThrow<User>()

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
                user[password] = BCrypt.hashpw(request.password, BCrypt.gensalt(BCRYPT_COST))
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
    call.respond(SuccessStatus("User '${request.username}' (ID: $userId) updated successfully."))
}

val deleteUserDoc: RouteDoc = {
    operationId = "deleteUser"
    summary = "Deletes an existing user."
    tags("User")
    parameters {
        pathParam("id", "The ID of the user that should be deleted.")
    }
    responses {
        json<SuccessStatus>(200)
        errors(401, 403, 404, 500)
    }
}

suspend fun deleteUser(call: ApplicationCall) {
    val userId = call.pathParam("id").toIntOrNull() ?: throw  ErrorStatusException(400, "Invalid user ID.")
    val count = transaction {
        Users.deleteWhere { Users.id eq userId }
    }
    if (count > 0) {
        call.respond(SuccessStatus("User  with ID$userId deleted successfully."))
    } else {
        throw ErrorStatusException(404, "User with ID $userId could not be found.")
    }
}