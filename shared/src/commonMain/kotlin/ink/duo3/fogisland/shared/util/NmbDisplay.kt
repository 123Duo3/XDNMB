package ink.duo3.fogisland.shared.util

private const val NMB_ANONYMOUS_NAME = "无名氏"
private const val NMB_UNTITLED = "无标题"

fun normalizeNmbDisplayText(value: String?): String? {
    return value?.trim()?.takeIf { it.isNotEmpty() }
}

fun normalizeNmbStoredName(name: String?): String {
    return name
        ?.trim()
        ?.takeUnless { it.isEmpty() || it == NMB_ANONYMOUS_NAME }
        .orEmpty()
}

fun normalizeNmbStoredTitle(title: String?): String {
    return title
        ?.trim()
        ?.takeUnless { it.isEmpty() || it == NMB_UNTITLED }
        .orEmpty()
}

fun resolveNmbDisplayTitle(title: String?): String? {
    return normalizeNmbStoredTitle(title).takeIf { it.isNotEmpty() }
}

fun resolveNmbDisplayAuthor(userHash: String?, name: String?): String? {
    return normalizeNmbDisplayText(userHash)
        ?: normalizeNmbStoredName(name).takeIf { it.isNotEmpty() }
}

fun resolveNmbCardNameIdString(forumName: String?, id: Long?): String =
    "No.$id · $forumName"