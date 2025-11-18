package ch.pontius.kiar.api.routes.publication

import ch.pontius.kiar.servers.sru.SruServer
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@OpenApi(
    path = "/api/{collection}/sru",
    methods = [HttpMethod.GET],
    summary = "An endpoint that provides SRU search for the specified collection.",
    operationId = "getSru",
    tags = ["SRU", "Publication"],
    queryParams = [
        OpenApiParam(name = "query", type = String::class, description = "The query string.", required = true),
        OpenApiParam(name = "maximumRecords", type = Int::class, description = "Number of records to return per page.", required = true),
        OpenApiParam(name = "startRecord", type = Int::class, description = "Page number (1-based).", required = true)
    ],
    pathParams = [
        OpenApiParam(name = "collection", type = String::class, description = "The collection to search.", required = true)
    ],
    responses = [
        OpenApiResponse("200", [OpenApiContent(mimeType = "text/xml")])
    ]
)
fun getSruSearch(ctx: Context, server: SruServer) {
    val doc = server.handle(ctx)

    /* Convert Document to XML string */
    val transformer = TransformerFactory.newInstance().newTransformer()
    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))
    val xmlString = writer.toString()

    /* Return XML response */
    ctx.contentType("text/xml").result(xmlString)
}