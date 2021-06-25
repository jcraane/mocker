package nl.jcraane.mocker.extensions

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.toMap
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.delay
import nl.jcraane.mocker.features.forwarding.QueryParam
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

private val log = LoggerFactory.getLogger("MockFunctions")

val UTF_8: Charset = Charset.forName("UTF-8")

suspend fun ApplicationCall.respondContents(
    classPathResource: String,
    contentType: ContentType? = null,
    httpStatusCode: HttpStatusCode = HttpStatusCode.OK
) {
    val resource = javaClass.getResource(classPathResource)
    if (resource != null) {
        respondBytes(
            bytes = resource.readBytes(),
            contentType = contentType ?: determineContentTypeOnFileExtensions(
                classPathResource
            ),
            status = httpStatusCode
        )
    } else {
        log.error(
            "Classpath resource [$classPathResource] cannot be found. Make sure it exists in src/main/resource${
                classPathResource.prependIfMissing(
                    "/"
                )
            } and starts with a '/' (forward slash) character."
        )
        respond(HttpStatusCode.InternalServerError)
    }
}

fun Application.mock(basePath: String = "", build: Route.() -> Unit): Routing {
    return routing {
        route(basePath) {
            apply(build)
        }
    }
}

suspend fun DefaultWebSocketServerSession.sendContinuousResponse(response: String, delayMillis: Long = 5000L) {
    while (true) {
        outgoing.send(Frame.Text(response))
        delay(delayMillis)
    }
}

suspend fun DefaultWebSocketServerSession.echoRequest() {
    for (frame in incoming) {
        when (frame) {
            is Frame.Text -> {
                val text = frame.readText()
                outgoing.send(Frame.Text("CLIENT SAID: $text"))
            }
        }
    }
}

fun Application.mockWebSocket(path: String = "", handler: suspend DefaultWebSocketServerSession.() -> Unit): Routing {
    return routing {
        webSocket(path) {
            handler()
        }
    }
}

private fun determineContentTypeOnFileExtensions(resource: String): ContentType {
    return if (resource.indexOf(".") != -1) {
        val extension = resource.substringAfterLast(".")
        when (extension) {
            "json" -> ContentType.Application.Json
            "pdf" -> ContentType.Application.Pdf
            "xml" -> ContentType.Application.Xml
            "html" -> ContentType.Text.Html
            "jpg" -> ContentType.Image.JPEG
            "jpeg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            "png" -> ContentType.Image.PNG
            else -> ContentType.Text.Plain
        }
    } else {
        ContentType.Text.Plain
    }
}

fun String.prependIfMissing(value: String): String {
    return if (!this.startsWith(value)) {
        "$value$this"
    } else {
        this
    }
}

fun getQueryParamNamePart(queryParameters: Set<QueryParam>): String {
    return buildString {
        queryParameters
            .map { "${it.name}=${it.value}" }
            .forEachIndexed { index, text ->
                val prefix = if (index == 0) "?" else "&"
                append(prefix)
                append(text)
            }
    }
}

fun getQueryParamsAsSet(parameters: Parameters) = parameters.toMap()
    .map { entry -> QueryParam(entry.key, entry.value.first()) }
    .toSet()

fun getQueryParamsAsMap(parameters: Parameters) = parameters.toMap()
    .map { it.key to it.value.first() }
    .toMap()
