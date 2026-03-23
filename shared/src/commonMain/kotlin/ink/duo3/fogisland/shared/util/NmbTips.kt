package ink.duo3.fogisland.shared.util

import kotlin.time.Instant

private const val NMB_TIPS_REMOTE_POST_ID = 9_999_999L
private const val NMB_TIPS_USER_HASH = "Tips"
private val NMB_TIPS_POSTED_AT = Instant.parse("2098-12-31T16:00:01Z")

fun isNmbTipsPost(userHash: String?, remotePostId: Long, postedAtRaw: String?): Boolean {
    if (normalizeNmbDisplayText(userHash) != NMB_TIPS_USER_HASH || remotePostId != NMB_TIPS_REMOTE_POST_ID) {
        return false
    }

    val parsedPostedAt = postedAtRaw?.let(::parseNmbPostedAt) ?: return false
    return parsedPostedAt == NMB_TIPS_POSTED_AT
}
