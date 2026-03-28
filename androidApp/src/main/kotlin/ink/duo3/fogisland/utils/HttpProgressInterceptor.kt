package ink.duo3.fogisland.utils

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

internal data class HttpTransferProgress(
    val url: String,
    val bytesRead: Long,
    val totalBytes: Long
) {
    val fraction: Float?
        get() = totalBytes
            .takeIf { it > 0L }
            ?.let { total ->
                (bytesRead.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
}

internal class HttpProgressInterceptor(
    private val onProgress: (HttpTransferProgress) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseBody = response.body ?: return response
        return response.newBuilder()
            .body(
                ProgressResponseBody(
                    responseBody = responseBody,
                    url = request.url.toString(),
                    onProgress = onProgress
                )
            )
            .build()
    }
}

private class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val url: String,
    private val onProgress: (HttpTransferProgress) -> Unit
) : ResponseBody() {
    private val bufferedSource by lazy {
        source(responseBody.source()).buffer()
    }

    override fun contentLength(): Long = responseBody.contentLength()

    override fun contentType() = responseBody.contentType()

    override fun source(): BufferedSource = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            private var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                val contentLength = contentLength()
                totalBytesRead = when {
                    bytesRead > 0L -> totalBytesRead + bytesRead
                    bytesRead == -1L && contentLength > 0L -> contentLength
                    else -> totalBytesRead
                }
                onProgress(
                    HttpTransferProgress(
                        url = url,
                        bytesRead = totalBytesRead,
                        totalBytes = contentLength
                    )
                )
                return bytesRead
            }
        }
    }
}
