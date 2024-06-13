package ch.pontius.kiar.api.routes.oai

import ch.pontius.kiar.api.model.oai.Verbs
import ch.pontius.kiar.api.model.status.ErrorStatus
import ch.pontius.kiar.api.model.status.SuccessStatus
import ch.pontius.kiar.api.model.oai.Verbs.*
import ch.pontius.kiar.oai.OaiServer
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@OpenApi(
    path = "/api/{collection}/oai-pmh",
    methods = [HttpMethod.GET, HttpMethod.POST],
    summary = "Attempts a login using the credentials provided in the request body.",
    operationId = "oaiPmh",
    tags = ["OAI"],
    queryParams = [
        OpenApiParam(name = "verb", type = String::class, description = "The OAI-PMH verb.", required = true),
        OpenApiParam(name = "resumptionToken", type = String::class, description = "The OAI-PMH resumption token (used for ListIdentifiers and ListRecords).", required = true),
    ],
    pathParams = [
        OpenApiParam(name = "collection", type = String::class, description = "The collection that should be harvested.", required = true),
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("500", [OpenApiContent(ErrorStatus::class)]),
    ]
)
fun oaiPmh(ctx: Context, server: OaiServer) {
    val verb = ctx.queryParam("verb")?.let { Verbs.valueOf(it.uppercase()) }
    val collection = ctx.pathParam("collection")

    /* Generate response document using OAI server. */
    val doc = when (verb) {
        IDENTIFY -> server.handleIdentify()
        LISTSETS -> TODO()
        LISTMETADATAFORMATS -> TODO()
        LISTIDENTIFIERS -> server.handleListIdentifiers(collection)
        LISTRECORDS -> TODO()
        GETRECORD -> TODO()
        null -> TODO()
    }

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    ctx.contentType("application/xml").result(xmlString)
}