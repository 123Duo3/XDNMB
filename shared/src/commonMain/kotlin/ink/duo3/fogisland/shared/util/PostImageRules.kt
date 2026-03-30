package ink.duo3.fogisland.shared.util

import ink.duo3.fogisland.shared.model.ThreadPostImage

const val MAX_POST_IMAGE_BYTES = 2048 * 1024
const val MAX_POST_IMAGE_LABEL = "2 MB"

fun ThreadPostImage?.isPostImageTooLarge(): Boolean {
    return this?.bytes?.size?.let { it > MAX_POST_IMAGE_BYTES } == true
}
