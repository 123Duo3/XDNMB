package ink.duo3.fogisland.shared.util

const val NMB_THREAD_REPLY_PAGE_SIZE = 19

fun calculateNmbThreadMaxPage(replyCount: Int): Int {
    if (replyCount <= 0) {
        return 1
    }

    return (replyCount + NMB_THREAD_REPLY_PAGE_SIZE - 1) / NMB_THREAD_REPLY_PAGE_SIZE
}
