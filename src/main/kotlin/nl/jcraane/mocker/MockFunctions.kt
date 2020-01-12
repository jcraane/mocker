package nl.jcraane.mocker

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing
import nl.jcraane.mocker.features.forwarding.QueryParam

suspend fun ApplicationCall.respondContents(
    classPathResource: String,
    contentType: ContentType? = null
) {
    respondText(
        javaClass.getResource(classPathResource).readText(),
        contentType ?: determineContentTypeOnFileExtensions(classPathResource)
    )
}

suspend fun ApplicationCall.respondFile(classPathResource: String, contentType: ContentType? = null) {
    val resource = javaClass.getResource(classPathResource)
    val bytes = resource.readBytes()
    respondBytes(bytes, contentType ?: determineContentTypeOnFileExtensions(classPathResource))
}

fun Application.mock(basePath: String = "", build: Route.() -> Unit): Routing {
    return routing {
        route(basePath) {
            apply(build)
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
