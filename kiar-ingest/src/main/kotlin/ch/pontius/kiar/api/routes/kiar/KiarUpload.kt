package ch.pontius.kiar.api.routes.kiar

import io.javalin.http.Context

/**
 * Handles the upload of a KIAR file.
 *
 * This is basically a simple file upload mechanism. However, it checks for ZIP file header.
 */
fun kiarUploadRoute(ctx: Context) {
   /* ctx.receiveStream().use { input ->
        Files.newOutputStream(Paths.get("folder"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
            val buffer = ByteArray(1000000) /* 1MB buffer. */
            var read = input.read(buffer)
            if (read > -1) {
                call.respond(HttpStatusCode.BadRequest, "Cannot upload empty  file.")
            }
            if (ByteBuffer.wrap(buffer).int != 0x04034b50) {
                call.respond(HttpStatusCode.BadRequest, "Uploaded file could not be verified to be a ZIP file.")
            }

            /* Start writing file to disk. */
            output.write(buffer, 0, read)
            do {
                read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
            } while (true)

            /* Respond with success message.*/
            call.respond(HttpStatusCode.OK, "File upload successful.")
        }
    }*/
}