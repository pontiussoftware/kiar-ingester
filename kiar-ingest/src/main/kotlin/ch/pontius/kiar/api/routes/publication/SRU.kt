package ch.pontius.kiar.api.routes.publication

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.openapi.*
import ch.pontius.kiar.servers.sru.SruServer
import ch.pontius.kiar.utilities.extensions.pathParam
import ch.pontius.kiar.utilities.extensions.queryParam
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

val getSruSearchDoc: RouteDoc = {
    operationId = "getSru"
    summary = "An endpoint that provides SRU search for the specified collection."
    tags("SRU", "Publication")
    parameters {
        pathParam("collection", "The collection to search.", STRING)
        queryParam("query", "The query string.", STRING, required = true)
        queryParam("maximumRecords", "Number of records to return per page.", INT32, required = true)
        queryParam("startRecord", "Page number (1-based).", INT32, required = true)
    }
    responses {
        xml(200)
    }
}

suspend fun getSruSearch(call: ApplicationCall, server: SruServer) {
    val collection = call.pathParam("collection")
    val query = call.queryParam("query") ?: "*"
    val pageSize = call.queryParam("maximumRecords")?.toIntOrNull() ?: 100
    val startRecord = call.queryParam("startRecord")?.toIntOrNull() ?: 1
    val doc = try {
        server.handle(collection, query, pageSize, startRecord)
    } catch (_: IllegalArgumentException) {
        throw ErrorStatusException(404, "Collection not found or not available via SRU.")
    }

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    call.respondText(xmlString, ContentType.Text.Xml)
}