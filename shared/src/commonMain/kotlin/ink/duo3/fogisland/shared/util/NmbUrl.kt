package ink.duo3.fogisland.shared.util

import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity

const val NMB_IMAGE_CDN_BASE_URL = "https://image.nmb.best/"

fun buildNmbImageUrl(image: String?, ext: String?): String? {
    val imageId = image?.trim().orEmpty()
    if (imageId.isEmpty()) {
        return null
    }

    if (imageId.startsWith("http://") || imageId.startsWith("https://")) {
        return imageId
    }

    val extension = ext?.trim().orEmpty()
    val normalizedExtension = when {
        extension.isEmpty() -> ""
        extension.startsWith(".") -> extension
        else -> ".$extension"
    }

    return "$NMB_IMAGE_CDN_BASE_URL$imageId$normalizedExtension"
}

val ThreadEntity.imageUrl: String?
    get() = buildNmbImageUrl(image = image, ext = ext)

val PostEntity.imageUrl: String?
    get() = buildNmbImageUrl(image = image, ext = ext)
