package ch.pontius.kiar.api.routes.oai

import ch.pontius.kiar.oai.OaiServer
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@OpenApi(
    path = "/api/{collection}/oai-pmh",
    methods = [HttpMethod.GET],
    summary = "An endpoint that provides OAI-PMH harvesting for the specified collection.",
    operationId = "getOaiPmh",
    tags = ["OAI"],
    queryParams = [
        OpenApiParam(name = "verb", type = String::class, description = "The OAI-PMH verb.", required = true),
        OpenApiParam(name = "identifier", type = String::class, description = "The identifier to harvest (used for GetRecord ).", required = false),
        OpenApiParam(name = "resumptionToken", type = String::class, description = "The OAI-PMH resumption token (used for ListIdentifiers and ListRecords).", required = false),
    ],
    pathParams = [
        OpenApiParam(name = "collection", type = String::class, description = "The collection to harvest.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(mimeType = "text/xml")])
    ]
)
fun getOaiPmh(ctx: Context, server: OaiServer) {
    val doc = server.handle(ctx)

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    ctx.contentType("text/xml").result(xmlString)
}

@OpenApi(
    path = "/api/{collection}/oai-pmh",
    methods = [HttpMethod.POST],
    summary = "An endpoint that provides OAI-PMH harvesting for the specified collection.",
    operationId = "postOaiPmh",
    tags = ["OAI"],
    formParams = [
        OpenApiParam(name = "verb", type = String::class, description = "The OAI-PMH verb.", required = true),
        OpenApiParam(name = "identifier", type = String::class, description = "The identifier to harvest (used for GetRecord ).", required = false),
        OpenApiParam(name = "resumptionToken", type = String::class, description = "The OAI-PMH resumption token (used for ListIdentifiers and ListRecords).", required = false),
    ],
    pathParams = [
        OpenApiParam(name = "collection", type = String::class, description = "The collection to harvest.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(mimeType = "text/xml")])
    ]
)
fun postOaiPmh(ctx: Context, server: OaiServer) {
    val doc = server.handle(ctx)

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    ctx.contentType("text/xml").result(xmlString)
}