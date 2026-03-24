package ink.duo3.fogisland.shared.util

private val PURE_THREAD_ID_REGEX = Regex("^\\d+$")
private val NO_THREAD_ID_REGEX = Regex("^(?:>>\\s*)?No\\.\\s*(\\d+)$", RegexOption.IGNORE_CASE)
private val THREAD_URL_REGEX = Regex(
    pattern = "^https?://(?:www\\.)?nmbxd(?:1)?\\.com/t/(\\d+)(?:[/?#].*)?$",
    option = RegexOption.IGNORE_CASE
)

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

    THREAD_URL_REGEX.matchEntire(normalizedInput)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it }

    return null
}
