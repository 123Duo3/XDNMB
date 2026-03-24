package ink.duo3.fogisland.shared.network.api

import ink.duo3.fogisland.shared.network.model.ForumGroupDto
import ink.duo3.fogisland.shared.network.model.NmbApiErrorDto
import ink.duo3.fogisland.shared.network.model.NoticeDto
import ink.duo3.fogisland.shared.network.model.TimelineDto
import ink.duo3.fogisland.shared.network.model.ThreadDto
import ink.duo3.fogisland.shared.storage.preferences.CookieManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

class NmbApiClient(
    private val cookieManager: CookieManager
) {
    companion object {
        private const val BACKUP_URL_PATH = "backupUrl"
        private const val NOTICE_PATH = "nmb-notice.json"
        private const val NOTICE_URL = "https://nmb.ovear.info/$NOTICE_PATH"

        private val bootstrapApiBaseUrls = listOf(
            "https://api.nmb.best/Api/",
            "https://www.nmbxd1.com/Api/"
        )
    }

    private val backupUrlMutex = Mutex()

    @Volatile
    private var activeBaseUrl = bootstrapApiBaseUrls.first()

    @Volatile
    private var knownBaseUrls = bootstrapApiBaseUrls

    @Volatile
    private var hasLoadedBackupUrls = false

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private suspend fun appendCookieHeader(builder: HttpRequestBuilder) {
        cookieManager.getActiveRequestCookieHeader()?.let { cookie ->
            builder.header(HttpHeaders.Cookie, cookie)
        }
    }

    suspend fun getForumList(): List<ForumGroupDto> {
        return requestBody("getForumList")
    }

    suspend fun getTimelineList(): List<TimelineDto> {
        return requestBody("getTimelineList")
    }

    suspend fun getForumThreads(fid: Long, page: Int): List<ThreadDto> {
        return requestBody("showf") {
            parameter("id", fid)
            parameter("page", page)
        }
    }

    suspend fun getTimelineThreads(id: Long, page: Int): List<ThreadDto> {
        return requestBody("timeline/$id") {
            parameter("page", page)
        }
    }

    suspend fun getThreadDetails(threadId: Long, page: Int): ThreadDto {
        return requestBody("thread") {
            parameter("id", threadId)
            parameter("page", page)
        }
    }

    suspend fun getNotice(): NoticeDto {
        return decodeResponse(
            response = client.get(NOTICE_URL),
            path = NOTICE_PATH
        )
    }

    private suspend inline fun <reified T> requestBody(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val attemptedBaseUrls = linkedSetOf<String>()
        var lastFailure: Throwable? = null

        tryDecodeCandidates<T>(
            path = path,
            configure = configure,
            baseUrls = candidateBaseUrls(),
            attemptedBaseUrls = attemptedBaseUrls
        ).onSuccess { return it }.onFailure { lastFailure = it }

        if (lastFailure is NmbApiResponseException) {
            throw lastFailure
        }

        if (path != BACKUP_URL_PATH) {
            try {
                refreshBackupUrls()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
            }
            tryDecodeCandidates<T>(
                path = path,
                configure = configure,
                baseUrls = candidateBaseUrls(),
                attemptedBaseUrls = attemptedBaseUrls
            ).onSuccess { return it }.onFailure { lastFailure = it }

            if (lastFailure is NmbApiResponseException) {
                throw lastFailure
            }
        }

        throw NmbApiException(
            path = path,
            attemptedBaseUrls = attemptedBaseUrls.toList(),
            cause = lastFailure
        )
    }

    private suspend inline fun <reified T> tryDecodeCandidates(
        path: String,
        configure: HttpRequestBuilder.() -> Unit,
        baseUrls: List<String>,
        attemptedBaseUrls: MutableSet<String>
    ): Result<T> {
        var lastFailure: Throwable? = null

        for (baseUrl in baseUrls) {
            if (!attemptedBaseUrls.add(baseUrl)) {
                continue
            }

            try {
                val response = client.get {
                    url.takeFrom(baseUrl + path)
                    appendCookieHeader(this)
                    configure()
                }
                val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl)
                val body = decodeResponse<T>(response, path)

                activeBaseUrl = normalizedBaseUrl
                if (!hasLoadedBackupUrls && path != BACKUP_URL_PATH) {
                    try {
                        refreshBackupUrls(preferredBaseUrl = normalizedBaseUrl)
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) {
                            throw throwable
                        }
                    }
                }
                return Result.success(body)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                if (throwable is NmbApiResponseException) {
                    return Result.failure(throwable)
                }
                lastFailure = throwable
            }
        }

        return Result.failure(lastFailure ?: IllegalStateException("没有可用的 API 域名"))
    }

    private fun candidateBaseUrls(): List<String> {
        val currentBaseUrl = activeBaseUrl
        return buildList {
            add(currentBaseUrl)
            addAll(knownBaseUrls.filterNot { it == currentBaseUrl })
        }
    }

    private suspend fun refreshBackupUrls(preferredBaseUrl: String? = null) {
        if (hasLoadedBackupUrls) {
            return
        }

        backupUrlMutex.withLock {
            if (hasLoadedBackupUrls) {
                return
            }

            val candidateBaseUrls = buildList {
                preferredBaseUrl?.let(::add)
                addAll(knownBaseUrls)
            }.distinct()

            for (baseUrl in candidateBaseUrls) {
                val backupUrls = try {
                    decodeResponse<List<String>>(
                        client.get {
                        url.takeFrom(baseUrl + BACKUP_URL_PATH)
                        appendCookieHeader(this)
                        },
                        BACKUP_URL_PATH
                    )
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    continue
                }

                knownBaseUrls = mergeBaseUrls(
                    listOf(baseUrl) + backupUrls.map(::normalizeApiBaseUrl)
                )
                activeBaseUrl = normalizeApiBaseUrl(baseUrl)
                hasLoadedBackupUrls = true
                return
            }
        }
    }

    private fun mergeBaseUrls(baseUrls: List<String>): List<String> {
        return (bootstrapApiBaseUrls + baseUrls)
            .map(::normalizeApiBaseUrl)
            .distinct()
    }

    private fun normalizeApiBaseUrl(baseUrl: String): String {
        val trimmedBaseUrl = baseUrl.trim().removeSuffix("/")
        return if (trimmedBaseUrl.endsWith("/Api")) {
            "$trimmedBaseUrl/"
        } else {
            "$trimmedBaseUrl/Api/"
        }
    }

    private suspend inline fun <reified T> decodeResponse(
        response: HttpResponse,
        path: String
    ): T {
        val bodyText = response.bodyAsText()

        try {
            val apiError = json.decodeFromString<NmbApiErrorDto>(bodyText)
            if (apiError.success == false) {
                val message = apiError.error?.takeIf { it.isNotBlank() } ?: "接口请求失败"
                throw NmbApiResponseException(path, message)
            }
        } catch (_: SerializationException) {
        }

        return try {
            json.decodeFromString(bodyText)
        } catch (throwable: SerializationException) {
            throw NmbApiParseException(path, throwable)
        }
    }
}

private class NmbApiException(
    path: String,
    attemptedBaseUrls: List<String>,
    cause: Throwable?
) : IllegalStateException(
    buildString {
        append("请求接口失败: ")
        append(path)
        if (attemptedBaseUrls.isNotEmpty()) {
            append("，已尝试 ")
            append(attemptedBaseUrls.joinToString())
        }
        when (cause) {
            is ClientRequestException,
            is ServerResponseException -> {
                append("，最后一次响应状态为 ")
                append(cause.response.status)
            }
            else -> {
                cause?.message?.takeIf { it.isNotBlank() }?.let { message ->
                    append("，最后一次错误为 ")
                    append(message)
                }
            }
        }
    },
    cause
)

private class NmbApiResponseException(
    path: String,
    message: String
) : IllegalStateException(
    message.ifBlank {
        "接口请求失败: $path"
    }
)

private class NmbApiParseException(
    path: String,
    cause: Throwable
) : IllegalStateException(
    "接口返回了无法解析的数据: $path",
    cause
)
