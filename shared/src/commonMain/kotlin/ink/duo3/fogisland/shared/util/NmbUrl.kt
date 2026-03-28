package ink.duo3.fogisland.shared.util

const val NMB_IMAGE_CDN_BASE_URL = "https://image.nmb.best"

private const val NMB_IMAGE_THUMB_SEGMENT = "/thumb/"
private const val NMB_IMAGE_FULL_SEGMENT = "/image/"

private fun normalizeNmbImagePath(image: String?, ext: String?): String? {
    val imageId = image?.trim().orEmpty()
    if (imageId.isEmpty()) {
        return null
    }

    val extension = ext?.trim().orEmpty()
    val normalizedExtension = when {
        extension.isEmpty() -> ""
        extension.startsWith(".") -> extension
        else -> ".$extension"
    }

    return "$imageId$normalizedExtension"
}

private fun String.isHttpUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}

private fun normalizeNmbImageCdnBaseUrl(cdnBaseUrl: String): String {
    return cdnBaseUrl
        .trim()
        .removeSuffix("/")
}

fun buildNmbThumbImageUrl(
    image: String?,
    ext: String?,
    cdnBaseUrl: String = NMB_IMAGE_CDN_BASE_URL
): String? {
    val imageId = image?.trim().orEmpty()
    if (imageId.isEmpty()) {
        return null
    }

    if (imageId.isHttpUrl()) {
        return if (NMB_IMAGE_FULL_SEGMENT in imageId) {
            imageId.replace(NMB_IMAGE_FULL_SEGMENT, NMB_IMAGE_THUMB_SEGMENT)
        } else {
            imageId
        }
    }

    val normalizedPath = normalizeNmbImagePath(image = imageId, ext = ext) ?: return null
    return "${normalizeNmbImageCdnBaseUrl(cdnBaseUrl)}$NMB_IMAGE_THUMB_SEGMENT$normalizedPath"
}

fun buildNmbFullImageUrl(
    image: String?,
    ext: String?,
    cdnBaseUrl: String = NMB_IMAGE_CDN_BASE_URL
): String? {
    val imageId = image?.trim().orEmpty()
    if (imageId.isEmpty()) {
        return null
    }

    if (imageId.isHttpUrl()) {
        return if (NMB_IMAGE_THUMB_SEGMENT in imageId) {
            imageId.replace(NMB_IMAGE_THUMB_SEGMENT, NMB_IMAGE_FULL_SEGMENT)
        } else {
            imageId
        }
    }

    val normalizedPath = normalizeNmbImagePath(image = imageId, ext = ext) ?: return null
    return "${normalizeNmbImageCdnBaseUrl(cdnBaseUrl)}$NMB_IMAGE_FULL_SEGMENT$normalizedPath"
}
