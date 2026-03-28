package ink.duo3.fogisland.utils

import ink.duo3.fogisland.shared.repository.ForumRepository

internal suspend fun resolveNmbImageFallbackUrl(
    repository: ForumRepository,
    currentUrl: String?,
    buildImageUrl: (String) -> String?
): String? {
    val fallbackBaseUrl = runCatching {
        repository.getImageCdnFallbackBaseUrl()
    }.getOrNull() ?: return null

    val fallbackUrl = buildImageUrl(fallbackBaseUrl)
        ?.takeUnless { it.isBlank() || it == currentUrl }

    return fallbackUrl
}
