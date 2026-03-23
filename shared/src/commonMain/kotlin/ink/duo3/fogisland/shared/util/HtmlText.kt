package ink.duo3.fogisland.shared.util

private val breakRegex = Regex("(?i)<br\\s*/?>")
private val paragraphCloseRegex = Regex("(?i)</p>")
private val blockCloseRegex = Regex("(?i)</div>|</li>|</tr>|</h[1-6]>")
private val tagRegex = Regex("<[^>]+>")
private val blankLineRegex = Regex("\\n{3,}")

private val htmlEntities = mapOf(
    "&nbsp;" to " ",
    "&gt;" to ">",
    "&lt;" to "<",
    "&amp;" to "&",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&bull;" to "•",
    "&ldquo;" to "\"",
    "&rdquo;" to "\"",
    "&hellip;" to "..."
)

fun htmlToPlainText(html: String?): String {
    if (html.isNullOrBlank()) {
        return ""
    }

    val text = htmlEntities.entries.fold(html) { acc, (entity, replacement) ->
        acc.replace(entity, replacement)
    }

    return text
        .replace(breakRegex, "\n")
        .replace(paragraphCloseRegex, "\n\n")
        .replace(blockCloseRegex, "\n")
        .replace(tagRegex, "")
        .lineSequence()
        .joinToString("\n") { it.trimEnd() }
        .replace(blankLineRegex, "\n\n")
        .trim()
}
