package ink.duo3.fogisland.shared.util

private val breakRegex = Regex("(?i)<br\\s*/?>")
private val paragraphCloseRegex = Regex("(?i)</p>")
private val blockCloseRegex = Regex("(?i)</div>|</li>|</tr>|</h[1-6]>")
private val tagRegex = Regex("<[^>]+>")
private val sourceLineBreakRegex = Regex("\\r\\n?|\\n")

private const val lineBreakPlaceholder = "\u0000FI_LINE_BREAK\u0000"
private const val paragraphBreakPlaceholder = "\u0000FI_PARAGRAPH_BREAK\u0000"

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
        .replace(breakRegex, lineBreakPlaceholder)
        .replace(paragraphCloseRegex, paragraphBreakPlaceholder)
        .replace(blockCloseRegex, lineBreakPlaceholder)
        .replace(tagRegex, "")
        // X 岛内容里的真实换行已经被编码成 <br> 等标签；保留这些语义换行，
        // 但忽略 HTML 源码本身为了排版携带的 CRLF/LF，避免每行都被额外拉开。
        .replace(sourceLineBreakRegex, "")
        .replace(paragraphBreakPlaceholder, "\n\n")
        .replace(lineBreakPlaceholder, "\n")
}
