package ch.pontius.kiar.api.routes.publication

import ch.pontius.kiar.servers.oai.OaiServer
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import io.ktor.server.application.ApplicationCall
import ch.pontius.kiar.api.openapi.*
import io.ktor.http.ContentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import ch.pontius.kiar.utilities.extensions.pathParam

val getOaiPmhDoc: RouteDoc = {
    operationId = "getOaiPmh"
    summary = "An endpoint that provides OAI-PMH harvesting for the specified collection."
    tags("OAI", "Publication")
    parameters {
        pathParam("collection", "The collection to harvest.", STRING)
        queryParam("verb", "The OAI-PMH verb.", STRING, required = true)
        queryParam("identifier", "The identifier to harvest (used for GetRecord ).")
        queryParam("resumptionToken", "The OAI-PMH resumption token (used for ListIdentifiers and ListRecords).")
        queryParam("metadataPrefix", "The OAI-PMH metadata prefix (used for GetRecord, ListIdentifiers and ListRecords).")
        queryParam("set", "The OAI-PMH set criterion for selective harvesting (used for ListIdentifiers and ListRecords).")
    }
    responses {
        xml(200)
    }
}

suspend fun getOaiPmh(call: ApplicationCall, server: OaiServer) {
    val parameters = call.request.queryParameters.entries().associate { it.key to it.value.first() }
    val doc = server.handle(call.pathParam("collection"), parameters)

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    call.respondText(xmlString, ContentType.Text.Xml)
}

val postOaiPmhDoc: RouteDoc = {
    operationId = "postOaiPmh"
    summary = "An endpoint that provides OAI-PMH harvesting for the specified collection."
    tags("OAI", "Publication")
    parameters {
        pathParam("collection", "The collection to harvest.", STRING)
    }
    formBody("Multipart form data containing the form fields to upload.", "verb", "identifier", "resumptionToken", "metadataPrefix", "set")
    responses {
        xml(200)
    }
}

suspend fun postOaiPmh(call: ApplicationCall, server: OaiServer) {
    val parameters = call.receiveParameters().entries().associate { it.key to it.value.first() }
    val doc = server.handle(call.pathParam("collection"), parameters)

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    call.respondText(xmlString, ContentType.Text.Xml)
}