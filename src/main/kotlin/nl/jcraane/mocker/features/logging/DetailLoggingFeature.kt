package nl.jcraane.mocker.features.logging

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.content.TextContent
import io.ktor.request.receiveText
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.ResponseHeaders
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DetailLoggingFeature(private val configuration: Configuration) {
    suspend fun intercept(context: PipelineContext<Any, ApplicationCall>) {
        val call = context.call

        if (configuration.logDetails.contains(Configuration.LogDetail.REQUEST_HEADERS)) {
            log(
                "REQUEST HEADERS",
                logger
            ) {
                logHeaders(call.request.headers)
            }
        }

        if (configuration.logDetails.contains(Configuration.LogDetail.RESPONSE_HEADERS)) {
            log(
                "RESPONSE HEADERS",
                logger
            ) {
                logResponseHeaders(call.response.headers)
            }
        }

        if (configuration.logDetails.contains(Configuration.LogDetail.REQUEST_BODY)) {
            val body = call.receiveText()
            if (body.isNotEmpty()) {
                log(
                    "REQUEST BODY",
                    logger
                ) {
                    logger.info(body)
                }
            }
        }

        if (configuration.logDetails.contains(Configuration.LogDetail.RESPONSE_BODY)) {
            val message = context.subject
            if (message is TextContent) {
                log(
                    "RESPONSE BODY",
                    logger
                ) {
                    logger.info(message.text)
                }
            }
        }
    }

    private fun logHeaders(headers: Headers) {
        headers.forEach { name, values ->
            values.forEach {
                logger.info("$name=$it")
            }
        }
    }

    private fun logResponseHeaders(headers: ResponseHeaders) {
        headers.allValues().forEach { name, values ->
            values.forEach {
                logger.info("$name=$it")
            }
        }
    }

    class Configuration {
        var logDetails = emptyList<LogDetail>()

        enum class LogDetail {
            REQUEST_HEADERS,
            RESPONSE_HEADERS,
            REQUEST_BODY,
            RESPONSE_BODY
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, DetailLoggingFeature> {
        val logger: Logger = LoggerFactory.getLogger("DetailLoggingFeature")
        override val key = AttributeKey<DetailLoggingFeature>("DetailLogging")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): DetailLoggingFeature {
            val detailLogging = DetailLoggingFeature(
                Configuration().apply(configure)
            )
            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
                detailLogging.intercept(this)
            }
            return detailLogging
        }

        fun log(detail: String, logger: Logger, block: () -> Unit) {
            logger.info("$detail START")
            block()
            logger.info("$detail END")
        }
    }
}