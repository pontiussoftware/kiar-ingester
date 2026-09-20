package ch.pontius.kiar.utilities.extensions

import ch.pontius.kiar.api.UserSession
import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.user.User
import ch.pontius.kiar.database.institutions.Users
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** The minimum length of a password. */
const val MIN_LENGTH_PASSWORD = 8

/** The minimum length of a username. */
const val MIN_LENGTH_USERNAME = 5

/** The bcrypt cost factor (log2 rounds) used when hashing passwords. A fresh salt is generated for every hash. */
const val BCRYPT_COST = 12

/**
 * Executes the given block on [Dispatchers.IO]. Used for blocking database and file system work within request handlers.
 *
 * @param block The block to execute.
 * @return The result of [block].
 */
suspend inline fun <T> blocking(crossinline block: () -> T): T = withContext(Dispatchers.IO) { block() }

/**
 * Parses the [ApplicationCall]s request body as type [T] and throws an [ErrorStatusException] if parsing fails.
 *
 * @return [T]
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveOrThrow(): T = try {
    this.receive<T>()
} catch (_: ContentTransformationException) {
    throw ErrorStatusException(400, "Failed to parse request body; malformed request.")
} catch (_: BadRequestException) {
    throw ErrorStatusException(400, "Failed to parse request body; malformed request.")
} catch (_: SerializationException) {
    throw ErrorStatusException(400, "Failed to parse request body; malformed request.")
}

/**
 * Returns the named path parameter of this [ApplicationCall].
 *
 * @param name The name of the path parameter.
 * @return The parameter value.
 */
fun ApplicationCall.pathParam(name: String): String = ((this as? RoutingCall)?.pathParameters ?: this.parameters)[name] ?: throw ErrorStatusException(400, "Missing path parameter '$name'.")

/**
 * Returns the named query parameter of this [ApplicationCall] or null, if it is absent.
 *
 * @param name The name of the query parameter.
 * @return The parameter value or null.
 */
fun ApplicationCall.queryParam(name: String): String? = this.request.queryParameters[name]

/** The attribute under which Ktor's [io.ktor.server.sessions.SessionTrackerById] keeps the current session ID (internal to Ktor, but stable). */
private val SESSION_ID_KEY = AttributeKey<String>("SessionId")

/**
 * A convenience method used to set the currently active [User] in the session.
 *
 * Any session that arrived with the request is invalidated first and detached from the call, so that Ktor issues a
 * fresh session ID for the authenticated session (prevents session fixation).
 */
suspend fun ApplicationCall.setUser(user: User) {
    val current = this.sessionId
    if (current != null) {
        this.sessions.clear<UserSession>(current)
        this.attributes.remove(SESSION_ID_KEY)
    }
    this.sessions.set(UserSession(user.id!!, user.username))
}

/**
 * A convenience method used to invalidate the currently active [User] in the session.
 */
fun ApplicationCall.invalidateUser() {
    this.sessions.clear<UserSession>()
}

/**
 * A convenience method used to access the currently active [User] from the session.
 *
 * Requires an ongoing database transaction.
 *
 * @return [User]
 */
fun ApplicationCall.currentUser(): User {
    val session = this.sessions.get<UserSession>() ?: throw ErrorStatusException(403, "Your are not logged in.")
    try {
        return Users.getById(session.userId) ?: throw ErrorStatusException(403, "Unable to find user associated with session.")
    } catch (e: Throwable) {
        this.sessions.clear<UserSession>()
        throw e
    }
}

/**
 * A file uploaded via multipart/form-data, buffered in a temporary file.
 *
 * @param fieldName The name of the form field the file was submitted with.
 * @param filename The original filename as submitted by the client.
 * @param path The [Path] to the temporary file holding the content.
 */
class UploadedFile(val fieldName: String?, val filename: String?, val path: Path) {
    /** Opens an [InputStream] to the uploaded content. */
    fun content(): InputStream = Files.newInputStream(this.path, StandardOpenOption.READ)

    /** Deletes the temporary file. */
    fun delete() {
        try {
            Files.deleteIfExists(this.path)
        } catch (_: IOException) {
            /* No op. */
        }
    }
}

/**
 * Reads all files submitted with this multipart/form-data request and buffers them in temporary files.
 *
 * @param fieldName Optional name of the form field to filter by.
 * @return [List] of [UploadedFile]s. The caller is responsible for deleting them.
 */
suspend fun ApplicationCall.uploadedFiles(fieldName: String? = null): List<UploadedFile> {
    val files = mutableListOf<UploadedFile>()
    val multipart = try {
        this.receiveMultipart(formFieldLimit = Long.MAX_VALUE)
    } catch (_: UnsupportedMediaTypeException) {
        return files
    }
    multipart.forEachPart { part ->
        try {
            if (part is PartData.FileItem && (fieldName == null || part.name == fieldName)) {
                val tmp = Files.createTempFile("kiar-upload-", ".tmp")
                withContext(Dispatchers.IO) {
                    part.provider().toInputStream().use { input ->
                        Files.newOutputStream(tmp).use { output -> input.copyTo(output) }
                    }
                }
                files.add(UploadedFile(part.name, part.originalFileName, tmp))
            }
        } finally {
            part.release()
        }
    }
    return files
}
