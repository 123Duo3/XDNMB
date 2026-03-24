package ink.duo3.fogisland.shared.network.api

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NmbApiErrorsTest {

    @Test
    fun networkErrorShowsFriendlyReasonAndRawDetail() {
        val presentation = buildNmbApiErrorPresentation(
            path = "getForumList",
            cause = UnknownHostException(
                "android_getaddrinfo failed: EAI_NODATA (No address associated with hostname)"
            )
        )

        assertEquals("DNS 解析失败", presentation.summary)
        assertTrue(
            presentation.detail?.contains("UnknownHostException") == true,
            presentation.detail
        )
    }

    @Test
    fun nestedTimeoutUsesRootCauseAsRawDetail() {
        val presentation = buildNmbApiErrorPresentation(
            path = "showf",
            cause = IllegalStateException(
                "outer failure",
                SocketTimeoutException("connect timed out")
            )
        )

        assertEquals("连接超时", presentation.summary)
        assertTrue(
            presentation.detail?.contains("SocketTimeoutException") == true,
            presentation.detail
        )
    }

    @Test
    fun parseErrorKeepsRawSerializationFailure() {
        val presentation = buildNmbApiErrorPresentation(
            path = "thread",
            cause = NmbApiParseException(
                path = "thread",
                cause = SerializationException("Unexpected JSON token")
            )
        )

        assertEquals("接口返回了无法解析的数据", presentation.summary)
        assertTrue(
            presentation.detail?.contains("Unexpected JSON token") == true,
            presentation.detail
        )
    }
}

private class UnknownHostException(
    message: String
) : IllegalStateException(message)

private class SocketTimeoutException(
    message: String
) : IllegalStateException(message)
