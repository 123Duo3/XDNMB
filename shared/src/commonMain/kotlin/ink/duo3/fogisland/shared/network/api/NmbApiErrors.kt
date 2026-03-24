package ink.duo3.fogisland.shared.network.api

import ink.duo3.fogisland.shared.model.ErrorPresentation
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

internal fun buildNmbApiExceptionMessage(
    path: String,
    cause: Throwable?
): String {
    val presentation = buildNmbApiErrorPresentation(path, cause)
    return presentation.detail?.let { detail ->
        "${presentation.summary}: $detail"
    } ?: presentation.summary
}

internal fun buildNmbApiErrorPresentation(
    path: String,
    cause: Throwable?
): ErrorPresentation {
    if (cause is NmbPresentableError) {
        return cause.presentation
    }

    val rawDetail = extractNmbRawFailureDetail(cause)

    describeNmbNetworkFailure(cause)?.let { category ->
        return ErrorPresentation(
            summary = category,
            detail = rawDetail
        )
    }

    return when (cause) {
        is ClientRequestException -> ErrorPresentation(
            summary = buildHttpFailureMessage(cause.response.status),
            detail = rawDetail
        )
        is ServerResponseException -> ErrorPresentation(
            summary = buildHttpFailureMessage(cause.response.status),
            detail = rawDetail
        )
        is NmbApiParseException -> ErrorPresentation(
            summary = "接口返回了无法解析的数据",
            detail = rawDetail
        )
        else -> ErrorPresentation(
            summary = "请求接口失败",
            detail = rawDetail
        )
    }
}

fun Throwable.toErrorPresentation(
    defaultSummary: String
): ErrorPresentation {
    return if (this is NmbPresentableError) {
        presentation
    } else {
        ErrorPresentation(
            summary = defaultSummary,
            detail = message?.takeIf { it.isNotBlank() && it != defaultSummary }
        )
    }
}

internal fun describeNmbNetworkFailure(cause: Throwable?): String? {
    if (cause == null) {
        return null
    }

    val haystack = buildList {
        var current: Throwable? = cause
        while (current != null) {
            current.message
                ?.lowercase()
                ?.let(::add)
            add(current.toString().lowercase())
            current = current.cause
        }
    }

    fun containsAny(vararg needles: String): Boolean {
        return haystack.any { text ->
            needles.any { needle -> text.contains(needle.lowercase()) }
        }
    }

    return when {
        containsAny(
            "unknownhostexception",
            "unresolvedaddressexception",
            "unable to resolve host",
            "no address associated with hostname",
            "name or service not known",
            "nodename nor servname provided",
            "eai_nodata",
            "eai_noname"
        ) -> "DNS 解析失败"

        containsAny(
            "connecttimeoutexception",
            "httprequesttimeoutexception",
            "sockettimeoutexception",
            "timed out",
            "timeout"
        ) -> "连接超时"

        containsAny(
            "sslexception",
            "sslhandshakeexception",
            "certificateexception",
            "handshake",
            "tls",
            "pkix"
        ) -> "TLS 握手失败"

        containsAny(
            "connectexception",
            "failed to connect",
            "connection refused",
            "network is unreachable",
            "no route to host",
            "ehostunreach",
            "enetunreach"
        ) -> "无法建立连接"

        containsAny(
            "eofexception",
            "unexpected end of stream",
            "connection reset",
            "reset by peer",
            "socket closed"
        ) -> "连接被中断"

        else -> null
    }
}

private fun buildHttpFailureMessage(status: HttpStatusCode): String {
    return when (status.value) {
        502 -> "网关错误（502）"
        503 -> "服务器暂时不可用（503）"
        504 -> "网关超时（504）"
        else -> "接口返回 ${status.value}"
    }
}

private fun extractNmbRawFailureDetail(cause: Throwable?): String? {
    val rootCause = generateSequence(cause) { it.cause }.lastOrNull() ?: return null
    return rootCause.toString().takeIf { it.isNotBlank() }
}

internal interface NmbPresentableError {
    val presentation: ErrorPresentation
}

private fun buildThrowableMessage(presentation: ErrorPresentation): String {
    return presentation.detail?.let { detail ->
        "${presentation.summary}: $detail"
    } ?: presentation.summary
}

internal class NmbApiException(
    override val presentation: ErrorPresentation,
    cause: Throwable?
) : IllegalStateException(
    buildThrowableMessage(presentation),
    cause
), NmbPresentableError {
    internal constructor(
        path: String,
        cause: Throwable?
    ) : this(
        presentation = buildNmbApiErrorPresentation(
            path = path,
            cause = cause
        ),
        cause = cause
    )
}

internal class NmbApiResponseException(
    override val presentation: ErrorPresentation
) : IllegalStateException(
    buildThrowableMessage(presentation)
), NmbPresentableError {
    internal constructor(
        path: String,
        message: String
    ) : this(
        ErrorPresentation(
            summary = message.ifBlank {
                "接口请求失败: $path"
            }
        )
    )
}

internal class NmbApiParseException(
    override val presentation: ErrorPresentation,
    cause: Throwable
) : IllegalStateException(
    buildThrowableMessage(presentation),
    cause
), NmbPresentableError {
    internal constructor(
        path: String,
        cause: Throwable
    ) : this(
        presentation = ErrorPresentation(
            summary = "接口返回了无法解析的数据",
            detail = extractNmbRawFailureDetail(cause)
        ),
        cause = cause
    )
}
