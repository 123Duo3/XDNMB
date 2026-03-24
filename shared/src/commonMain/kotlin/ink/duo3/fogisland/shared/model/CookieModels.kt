package ink.duo3.fogisland.shared.model

import kotlinx.serialization.Serializable

const val MAX_COOKIE_PROFILE_COUNT = 5

@Serializable
data class CookieProfile(
    val id: String,
    val cookieValue: String,
    val accountName: String? = null,
    val remark: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val displayName: String?
        get() = accountName?.takeIf { it.isNotBlank() }
}

data class CookieCollection(
    val cookies: List<CookieProfile>,
    val activeRequestCookieId: String?,
    val activePostCookieId: String?
)

data class CookieImportPayload(
    val cookieValue: String,
    val accountName: String? = null
)
