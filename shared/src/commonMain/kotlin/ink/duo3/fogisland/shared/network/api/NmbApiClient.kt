package ink.duo3.fogisland.shared.network.api

import ink.duo3.fogisland.shared.model.ReplyPostRequest
import ink.duo3.fogisland.shared.model.ReplyPostResult
import ink.duo3.fogisland.shared.model.ThreadPostImage
import ink.duo3.fogisland.shared.model.ThreadPostRequest
import ink.duo3.fogisland.shared.model.ThreadPostResult
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import ink.duo3.fogisland.shared.network.model.CdnPathDto
import ink.duo3.fogisland.shared.network.model.ForumGroupDto
import ink.duo3.fogisland.shared.network.model.LastPostDto
import ink.duo3.fogisland.shared.network.model.NmbApiErrorDto
import ink.duo3.fogisland.shared.network.model.NoticeDto
import ink.duo3.fogisland.shared.network.model.TimelineDto
import ink.duo3.fogisland.shared.network.model.ThreadDto
import ink.duo3.fogisland.shared.storage.preferences.CookieManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
import io.ktor.http.content.PartData
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.serialization.kotlinx.json.json
import ink.duo3.fogisland.shared.util.normalizeNmbStoredName
import ink.duo3.fogisland.shared.util.normalizeNmbStoredTitle
import ink.duo3.fogisland.shared.util.htmlToPlainText
import kotlinx.io.Buffer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.concurrent.Volatile

class NmbApiClient(
    private val cookieManager: CookieManager
) {
    companion object {
        private const val BACKUP_URL_PATH = "Api/backupUrl"
        private const val GET_LAST_POST_PATH = "Api/getLastPost"
        private const val FORUM_LIST_PATH = "Api/getForumList"
        private const val TIMELINE_LIST_PATH = "Api/getTimelineList"
        private const val CDN_PATH_PATH = "Api/getCdnPath"
        private const val SHOW_FORUM_PATH = "Api/showf"
        private const val THREAD_DETAILS_PATH = "Api/thread"
        private const val FEED_PATH = "Api/feed/"
        private const val ADD_FEED_PATH = "Api/addFeed"
        private const val DELETE_FEED_PATH = "Api/delFeed"
        private const val NOTICE_PATH = "nmb-notice.json"
        private const val NOTICE_URL = "https://nmb.ovear.info/$NOTICE_PATH"
        private const val POST_THREAD_PATH = "Home/Forum/doPostThread.html"
        private const val POST_REPLY_PATH = "Home/Forum/doReplyThread.html"
        private const val POST_SUCCESS_POLL_COUNT = 4
        private const val POST_SUCCESS_POLL_DELAY_MILLIS = 1_000L

        private val bootstrapApiBaseUrls = listOf(
            "https://api.nmb.best/",
            "https://www.nmbxd1.com/"
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
        appendCommonHeaders(builder)
        cookieManager.getActiveRequestCookieHeader()?.let { cookie ->
            builder.header(HttpHeaders.Cookie, cookie)
        }
    }

    private suspend fun appendPostCookieHeader(builder: HttpRequestBuilder) {
        appendCommonHeaders(builder)
        cookieManager.getActivePostCookieHeader()?.let { cookie ->
            builder.header(HttpHeaders.Cookie, cookie)
        }
    }

    private fun appendCommonHeaders(builder: HttpRequestBuilder) {
        builder.header(HttpHeaders.UserAgent, fogIslandUserAgent())
    }

    suspend fun getForumList(): List<ForumGroupDto> {
        return requestBody(FORUM_LIST_PATH)
    }

    suspend fun getTimelineList(): List<TimelineDto> {
        return requestBody(TIMELINE_LIST_PATH)
    }

    suspend fun getForumThreads(fid: Long, page: Int): List<ThreadDto> {
        return requestBody(SHOW_FORUM_PATH) {
            parameter("id", fid)
            parameter("page", page)
        }
    }

    suspend fun getTimelineThreads(id: Long, page: Int): List<ThreadDto> {
        return requestBody("Api/timeline/$id") {
            parameter("page", page)
        }
    }

    suspend fun getThreadDetails(threadId: Long, page: Int): ThreadDto {
        return requestBody(THREAD_DETAILS_PATH) {
            parameter("id", threadId)
            parameter("page", page)
        }
    }

    suspend fun getFeedThreads(uuid: String, page: Int): List<ThreadDto> {
        return requestBody(FEED_PATH) {
            parameter("uuid", uuid)
            parameter("page", page)
        }
    }

    suspend fun addFeed(uuid: String, threadId: Long): String {
        return requestBody(ADD_FEED_PATH) {
            parameter("uuid", uuid)
            parameter("tid", threadId)
        }
    }

    suspend fun deleteFeed(uuid: String, threadId: Long): String {
        return requestBody(DELETE_FEED_PATH) {
            parameter("uuid", uuid)
            parameter("tid", threadId)
        }
    }

    suspend fun getNotice(): NoticeDto {
        return decodeResponse(
            response = client.get {
                url.takeFrom(NOTICE_URL)
                appendCommonHeaders(this)
            },
            path = NOTICE_PATH
        )
    }

    suspend fun getPreferredImageCdnBaseUrl(): String? {
        return runCatching {
            requestBody<List<CdnPathDto>>(CDN_PATH_PATH)
        }.getOrNull()
            ?.sortedByDescending { it.rate ?: 0.0 }
            ?.mapNotNull { it.url?.trim()?.takeIf { value -> value.isNotBlank() } }
            ?.firstOrNull()
            ?.let(::normalizeImageCdnBaseUrl)
    }

    suspend fun postThread(request: ThreadPostRequest): ThreadPostResult {
        val response = client.submitFormWithBinaryData(
            url = activeBaseUrl + POST_THREAD_PATH,
            formData = buildThreadPostFormData(request)
        ) {
            appendPostCookieHeader(this)
        }

        return decodeThreadPostResponse(
            response = response,
            request = request
        )
    }

    suspend fun postReply(request: ReplyPostRequest): ReplyPostResult {
        val response = client.submitFormWithBinaryData(
            url = activeBaseUrl + POST_REPLY_PATH,
            formData = buildReplyPostFormData(request)
        ) {
            appendPostCookieHeader(this)
        }

        return decodeReplyPostResponse(
            response = response,
            request = request
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
                attemptedBaseUrls = attemptedBaseUrls,
                previousFailure = lastFailure
            ).onSuccess { return it }.onFailure { lastFailure = it }

            if (lastFailure is NmbApiResponseException) {
                throw lastFailure
            }
        }

        throw NmbApiException(
            path = path,
            cause = lastFailure
        )
    }

    private suspend inline fun <reified T> tryDecodeCandidates(
        path: String,
        configure: HttpRequestBuilder.() -> Unit,
        baseUrls: List<String>,
        attemptedBaseUrls: MutableSet<String>,
        previousFailure: Throwable? = null
    ): Result<T> {
        var lastFailure: Throwable? = previousFailure

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
                val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
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

        return Result.failure(lastFailure ?: IllegalStateException("请求前未找到可用的 API 域名"))
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
                    listOf(baseUrl) + backupUrls.map(::normalizeBaseUrl)
                )
                activeBaseUrl = normalizeBaseUrl(baseUrl)
                hasLoadedBackupUrls = true
                return
            }
        }
    }

    private fun mergeBaseUrls(baseUrls: List<String>): List<String> {
        return (bootstrapApiBaseUrls + baseUrls)
            .map(::normalizeBaseUrl)
            .distinct()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return baseUrl
            .trim()
            .removeSuffix("/")
            .removeSuffix("/Api")
            .removeSuffix("/")
            .plus("/")
    }

    private fun normalizeImageCdnBaseUrl(baseUrl: String): String {
        return baseUrl
            .trim()
            .removeSuffix("/")
            .removeSuffix("/thumb")
            .removeSuffix("/image")
            .removeSuffix("/")
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

        detectNmbBusinessErrorMessage(
            json = json,
            bodyText = bodyText,
            expectsRawString = T::class == String::class
        )?.let { message ->
            throw NmbApiResponseException(path, message)
        }

        return try {
            json.decodeFromString(bodyText)
        } catch (throwable: SerializationException) {
            throw NmbApiParseException(path, throwable)
        }
    }

    private suspend fun decodeThreadPostResponse(
        response: HttpResponse,
        request: ThreadPostRequest
    ): ThreadPostResult {
        val bodyText = response.bodyAsText()
        throwIfPostFormFailed(path = POST_THREAD_PATH, bodyText = bodyText)

        val redirectedThreadId = extractThreadIdFromUrl(response.call.request.url.toString())
        val htmlSuccessMessage = extractHtmlFormSuccessMessage(bodyText)
        val confirmedPost = resolveOptionalPostConfirmation(
            primarySuccess = redirectedThreadId != null || htmlSuccessMessage != null,
            confirmationResult = runCatching { awaitPostedThreadOrNull(request) }
        )
        val threadId = redirectedThreadId ?: confirmedPost?.id
        val successMessage = htmlSuccessMessage
            ?: threadId?.let { "发串成功" }
            ?: throw NmbApiParseException(
                POST_THREAD_PATH,
                IllegalStateException("未识别到发串结果")
            )

        return ThreadPostResult(
            forumId = request.forumId,
            threadId = threadId,
            message = successMessage,
            contentText = resolveCanonicalPostedContent(
                confirmedPost = confirmedPost,
                requestContent = request.content
            )
        )
    }

    private suspend fun decodeReplyPostResponse(
        response: HttpResponse,
        request: ReplyPostRequest
    ): ReplyPostResult {
        val bodyText = response.bodyAsText()
        throwIfPostFormFailed(path = POST_REPLY_PATH, bodyText = bodyText)

        val htmlSuccessMessage = extractHtmlFormSuccessMessage(bodyText)
        val confirmedPost = resolveOptionalPostConfirmation(
            primarySuccess = htmlSuccessMessage != null,
            confirmationResult = runCatching { awaitPostedReplyOrNull(request) }
        )
        val successMessage = htmlSuccessMessage
            ?: confirmedPost?.let { "回复成功" }
            ?: throw NmbApiParseException(
                POST_REPLY_PATH,
                IllegalStateException("未识别到回帖结果")
            )

        return ReplyPostResult(
            threadId = request.threadId,
            postId = confirmedPost?.id,
            message = successMessage,
            contentText = resolveCanonicalPostedContent(
                confirmedPost = confirmedPost,
                requestContent = request.content
            )
        )
    }

    private suspend fun awaitPostedThreadOrNull(request: ThreadPostRequest): LastPostDto? {
        repeat(POST_SUCCESS_POLL_COUNT) { attempt ->
            fetchLastPostOrNull()
                ?.takeIf { it.resto == null || it.resto == 0L }
                ?.takeIf { it.matchesThreadPostRequest(request) }
                ?.let { return it }

            if (attempt < POST_SUCCESS_POLL_COUNT - 1) {
                delay(POST_SUCCESS_POLL_DELAY_MILLIS)
            }
        }

        return null
    }

    private suspend fun awaitPostedReplyOrNull(request: ReplyPostRequest): LastPostDto? {
        repeat(POST_SUCCESS_POLL_COUNT) { attempt ->
            fetchLastPostOrNull()
                ?.takeIf { it.resto == request.threadId }
                ?.takeIf { it.matchesReplyPostRequest(request) }
                ?.let { return it }

            if (attempt < POST_SUCCESS_POLL_COUNT - 1) {
                delay(POST_SUCCESS_POLL_DELAY_MILLIS)
            }
        }

        return null
    }

    private suspend fun fetchLastPostOrNull(): LastPostDto? {
        val response = client.get {
            url.takeFrom(activeBaseUrl + GET_LAST_POST_PATH)
            appendPostCookieHeader(this)
        }
        val bodyText = response.bodyAsText().trim()
        if (bodyText == "[]" || bodyText == "null") {
            return null
        }

        detectNmbApiErrorDtoMessage(bodyText)?.let { message ->
            throw NmbApiResponseException(GET_LAST_POST_PATH, message)
        }
        detectNmbBusinessErrorMessage(
            json = json,
            bodyText = bodyText,
            expectsRawString = false
        )?.let { message ->
            throw NmbApiResponseException(GET_LAST_POST_PATH, message)
        }

        return try {
            json.decodeFromString<LastPostDto>(bodyText)
        } catch (throwable: SerializationException) {
            throw NmbApiParseException(GET_LAST_POST_PATH, throwable)
        }
    }

    private fun buildThreadPostFormData(request: ThreadPostRequest): List<PartData> {
        return buildList {
            addFormItem("fid", request.forumId.toString())
            addFormItem("name", request.name)
            addFormItem("email", request.email)
            addFormItem("title", request.title)
            addFormItem("content", request.content)
            if (request.image != null && request.useWatermark) {
                addFormItem("water", "true")
            }
            request.image?.let { image ->
                addImagePart(image)
            }
        }
    }

    private fun buildReplyPostFormData(request: ReplyPostRequest): List<PartData> {
        return buildList {
            addFormItem("resto", request.threadId.toString())
            addFormItem("name", request.name)
            addFormItem("email", request.email)
            addFormItem("title", request.title)
            addFormItem("content", request.content)
            if (request.image != null && request.useWatermark) {
                addFormItem("water", "true")
            }
            request.image?.let { image ->
                addImagePart(image)
            }
        }
    }

    private fun MutableList<PartData>.addFormItem(
        key: String,
        value: String
    ) {
        add(
            PartData.FormItem(
                value = value,
                dispose = {},
                partHeaders = buildMultipartDispositionHeaders(name = key)
            )
        )
    }

    private fun MutableList<PartData>.addImagePart(image: ThreadPostImage) {
        add(
            PartData.BinaryItem(
                provider = {
                    Buffer().apply {
                        write(image.bytes)
                    }
                },
                dispose = {},
                partHeaders = buildMultipartDispositionHeaders(
                    name = "image",
                    fileName = image.fileName.escapeForContentDisposition(),
                    contentType = image.mimeType
                        ?.takeIf { it.isNotBlank() }
                        ?.let(ContentType::parse),
                    contentLength = image.bytes.size.toLong()
                )
            )
        )
    }

    private fun buildMultipartDispositionHeaders(
        name: String,
        fileName: String? = null,
        contentType: ContentType? = null,
        contentLength: Long? = null
    ): Headers {
        return Headers.build {
            append(
                HttpHeaders.ContentDisposition,
                buildString {
                    append("""form-data; name="$name"""")
                    fileName?.let {
                        append("""; filename="$it"""")
                    }
                }
            )
            contentType?.let {
                append(HttpHeaders.ContentType, it.toString())
            }
            contentLength?.let {
                append(HttpHeaders.ContentLength, it.toString())
            }
        }
    }
}

private fun throwIfPostFormFailed(
    path: String,
    bodyText: String
) {
    detectNmbApiErrorDtoMessage(bodyText)?.let { message ->
        throw NmbApiResponseException(path, message)
    }
    detectNmbBusinessErrorMessage(
        json = nmbApiDetectionJson,
        bodyText = bodyText,
        expectsRawString = false
    )?.let { message ->
        throw NmbApiResponseException(path, message)
    }
    extractHtmlFormErrorMessage(bodyText)?.let { message ->
        throw NmbApiResponseException(path, message)
    }
}

internal fun <T> resolveOptionalPostConfirmation(
    primarySuccess: Boolean,
    confirmationResult: Result<T>
): T? {
    if (confirmationResult.isSuccess) {
        return confirmationResult.getOrNull()
    }
    confirmationResult.exceptionOrNull()?.let { throwable ->
        if (throwable is CancellationException) {
            throw throwable
        }
    }
    if (primarySuccess) {
        return null
    }
    throw confirmationResult.exceptionOrNull()
        ?: IllegalStateException("未识别到发帖结果")
}

internal fun detectNmbBusinessErrorMessage(
    json: Json,
    bodyText: String,
    expectsRawString: Boolean
): String? {
    if (expectsRawString) {
        return null
    }

    val primitive = try {
        json.parseToJsonElement(bodyText) as? JsonPrimitive
    } catch (_: SerializationException) {
        null
    } ?: return null

    if (!primitive.isString) {
        return null
    }

    return primitive.contentOrNull?.takeIf { it.isNotBlank() }
}

private val nmbApiDetectionJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

internal fun detectNmbApiErrorDtoMessage(bodyText: String): String? {
    return try {
        nmbApiDetectionJson.decodeFromString<NmbApiErrorDto>(bodyText)
    } catch (_: SerializationException) {
        null
    }?.takeIf { it.success == false }
        ?.error
        ?.takeIf { it.isNotBlank() }
}

private val HTML_FORM_ERROR_REGEX = Regex("""<p class="error">\s*([^<]+?)\s*</p>""")
private val HTML_FORM_SUCCESS_REGEX = Regex("""<p class="success">\s*([^<]+?)\s*</p>""")
private val THREAD_URL_REGEX = Regex("""https?://[^"' ]+/t/(\d+)""")

internal fun extractHtmlFormErrorMessage(bodyText: String): String? {
    return HTML_FORM_ERROR_REGEX.find(bodyText)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

internal fun extractHtmlFormSuccessMessage(bodyText: String): String? {
    return HTML_FORM_SUCCESS_REGEX.find(bodyText)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

internal fun extractThreadIdFromUrl(url: String): Long? {
    return THREAD_URL_REGEX.find(url)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
}

private fun LastPostDto.matchesThreadPostRequest(request: ThreadPostRequest): Boolean {
    val normalizedRequestTitle = normalizeNmbStoredTitle(request.title).orEmpty().trim()
    val normalizedLastTitle = normalizeNmbStoredTitle(title).orEmpty().trim()
    if (normalizedRequestTitle != normalizedLastTitle) {
        return false
    }

    val normalizedRequestName = normalizeNmbStoredName(request.name).orEmpty().trim()
    val normalizedLastName = normalizeNmbStoredName(name).orEmpty().trim()
    if (normalizedRequestName != normalizedLastName) {
        return false
    }

    if (email.orEmpty().trim() != request.email.trim()) {
        return false
    }

    return normalizeComparablePostContent(content.orEmpty()) in
        comparablePostedContentCandidates(
            requestContent = request.content,
            hasImage = request.image != null
        )
}

private fun LastPostDto.matchesReplyPostRequest(request: ReplyPostRequest): Boolean {
    val normalizedRequestTitle = normalizeNmbStoredTitle(request.title).orEmpty().trim()
    val normalizedLastTitle = normalizeNmbStoredTitle(title).orEmpty().trim()
    if (normalizedRequestTitle != normalizedLastTitle) {
        return false
    }

    val normalizedRequestName = normalizeNmbStoredName(request.name).orEmpty().trim()
    val normalizedLastName = normalizeNmbStoredName(name).orEmpty().trim()
    if (normalizedRequestName != normalizedLastName) {
        return false
    }

    if (email.orEmpty().trim() != request.email.trim()) {
        return false
    }

    return normalizeComparablePostContent(content.orEmpty()) in
        comparablePostedContentCandidates(
            requestContent = request.content,
            hasImage = request.image != null
        )
}

internal fun resolveCanonicalPostedContent(
    confirmedPost: LastPostDto?,
    requestContent: String
): String {
    return confirmedPost
        ?.content
        ?.let(::htmlToPlainText)
        ?: requestContent
}

internal fun normalizeComparablePostContent(content: String): String {
    return content
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        // 实测服务端会裁掉正文最外层的空白行，并吞掉正文整体首尾的半角空格；
        // 但不会吞掉正文内部的空格、Tab、全角空格或空白行。
        .replace(Regex("^\n+"), "")
        .replace(Regex("\n+$"), "")
        .replace(Regex("^ +"), "")
        .replace(Regex(" +$"), "")
}

internal fun comparablePostedContentCandidates(
    requestContent: String,
    hasImage: Boolean
): Set<String> {
    val normalizedRequestContent = normalizeComparablePostContent(requestContent)
    if (!hasImage || normalizedRequestContent.isNotEmpty()) {
        return setOf(normalizedRequestContent)
    }

    return setOf(
        normalizedRequestContent,
        normalizeComparablePostContent("分享图片")
    )
}

private fun String.escapeForContentDisposition(): String {
    return replace("\\", "_").replace("\"", "_")
}
