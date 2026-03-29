package ink.duo3.fogisland.shared.util

private const val NMB_ANONYMOUS_NAME = "无名氏"
private const val NMB_UNTITLED = "无标题"

fun normalizeNmbStoredName(name: String?): String? {
    return name
        ?.trim()
        ?.takeUnless { it.isEmpty() || it == NMB_ANONYMOUS_NAME }
}

fun normalizeNmbStoredTitle(title: String?): String? {
    return title
        ?.trim()
        ?.takeUnless { it.isEmpty() || it == NMB_UNTITLED }
}
