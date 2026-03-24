package ink.duo3.fogisland.shared.util

import ink.duo3.fogisland.shared.model.CookieImportPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val cookieImportJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun parseNmbCookieImportPayload(rawText: String): CookieImportPayload {
    val normalizedText = rawText.trim()
    require(normalizedText.isNotEmpty()) { "饼干内容不能为空" }

    if (normalizedText.startsWith("{")) {
        val payload = try {
            cookieImportJson.decodeFromString<CookieQrPayload>(normalizedText)
        } catch (throwable: SerializationException) {
            throw IllegalArgumentException("无法解析饼干二维码内容", throwable)
        }

        val cookieValue = payload.cookie?.trim().orEmpty()
        require(cookieValue.isNotEmpty()) { "二维码里没有饼干内容" }

        return CookieImportPayload(
            cookieValue = cookieValue,
            accountName = payload.name?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    return CookieImportPayload(cookieValue = normalizedText)
}

@Serializable
private data class CookieQrPayload(
    @SerialName("cookie") val cookie: String? = null,
    @SerialName("name") val name: String? = null
)
