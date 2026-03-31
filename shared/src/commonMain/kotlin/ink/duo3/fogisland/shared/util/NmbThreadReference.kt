package ink.duo3.fogisland.shared.util

private val PURE_THREAD_ID_REGEX = Regex("^\\d+$")
private val NO_THREAD_ID_REGEX = Regex("^(?:>>\\s*)?No\\.\\s*(\\d+)$", RegexOption.IGNORE_CASE)
private val THREAD_URL_REGEX = Regex(
    pattern = "^https?://(?:www\\.)?nmbxd(?:1)?\\.com/t/(\\d+)(?:[/?#].*)?$",
    option = RegexOption.IGNORE_CASE
)
private val ABSOLUTE_URL_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.+$")

private data class ParsedNmbThreadUrl(
    val threadId: Long,
    val targetPostId: Long?,
    val targetPage: Int?
)

sealed interface NmbLinkTarget {
    data class PostReference(val postId: Long) : NmbLinkTarget
    data class Thread(
        val threadId: Long,
        val targetPostId: Long? = null,
        val targetPage: Int? = null
    ) : NmbLinkTarget
    data class ExternalUrl(val url: String) : NmbLinkTarget
}

fun parseNmbThreadIdInput(input: String): Long? {
    val normalizedInput = input.trim()
    if (normalizedInput.isEmpty()) {
        return null
    }

    if (PURE_THREAD_ID_REGEX.matches(normalizedInput)) {
        return normalizedInput.toLongOrNull()
    }

    NO_THREAD_ID_REGEX.matchEntire(normalizedInput)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it }

    parseNmbThreadUrl(normalizedInput)?.let { return it }

    return null
}

fun parseNmbThreadUrl(input: String): Long? {
    return parseNmbThreadUrlTarget(input)?.threadId
}

fun resolveNmbUrlLinkTarget(input: String): NmbLinkTarget? {
    val normalizedInput = input.trim()
    if (normalizedInput.isEmpty()) {
        return null
    }

    parseNmbThreadUrlTarget(normalizedInput)?.let { target ->
        return target
    }

    if (ABSOLUTE_URL_REGEX.matches(normalizedInput)) {
        return NmbLinkTarget.ExternalUrl(normalizedInput)
    }

    return null
}

private fun parseNmbThreadUrlTarget(input: String): NmbLinkTarget.Thread? {
    val normalizedInput = input.trim()
    val threadId = THREAD_URL_REGEX.matchEntire(normalizedInput)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?: return null

    val parsedUrl = ParsedNmbThreadUrl(
        threadId = threadId,
        targetPostId = parseNmbThreadUrlQueryParameter(normalizedInput, "r")?.toLongOrNull(),
        targetPage = parseNmbThreadUrlQueryParameter(normalizedInput, "page")?.toIntOrNull()
    )

    return NmbLinkTarget.Thread(
        threadId = parsedUrl.threadId,
        targetPostId = parsedUrl.targetPostId,
        targetPage = parsedUrl.targetPage
    )
}

private fun parseNmbThreadUrlQueryParameter(url: String, name: String): String? {
    val query = url.substringAfter('?', "")
        .substringBefore('#')
        .takeIf { it.isNotEmpty() }
        ?: return null

    return query.split('&')
        .asSequence()
        .map { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            key to value
        }
        .firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }
        ?.second
        ?.takeIf { it.isNotBlank() }
}
