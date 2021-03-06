package nl.jcraane.mocker

import io.ktor.http.ContentType
import nl.jcraane.mocker.extensions.getQueryParamNamePart
import nl.jcraane.mocker.extensions.isSupportedTextContentType
import nl.jcraane.mocker.extensions.prependIfMissing
import nl.jcraane.mocker.features.forwarding.QueryParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockFunctionsTest {
    @Test
    fun getQueryParamsNamePart() {
        assertEquals("", getQueryParamNamePart(emptySet()))
        assertEquals("?name=value",
            getQueryParamNamePart(setOf(QueryParam("name" to "value")))
        )
        assertEquals("?name=value&details=true", getQueryParamNamePart(
            setOf(
                QueryParam("name" to "value"),
                QueryParam("details" to "true")
            )
        )
        )
    }

    @Test
    fun prependIfMissing() {
        assertEquals("/test", "test".prependIfMissing("/"))
        assertEquals("/test", "/test".prependIfMissing("/"))
        assertEquals("/test", "/test".prependIfMissing(""))
        assertEquals("test", "test".prependIfMissing(""))
    }

    @Test
    fun isTextContentType() {
        assertTrue(ContentType.Application.Json.isSupportedTextContentType())
//        todo add more tests
    }
}