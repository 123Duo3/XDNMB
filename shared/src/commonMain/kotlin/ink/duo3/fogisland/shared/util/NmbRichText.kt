package ink.duo3.fogisland.shared.util

data class NmbRichText(
    val segments: List<NmbRichTextSegment>
) {
    val plainText: String
        get() = buildString {
            segments.forEach { segment ->
                append(segment.text)
            }
        }
}

data class NmbRichTextSegment(
    val text: String,
    val color: String? = null,
    val semanticColor: NmbRichTextSemanticColor? = null,
    val linkTarget: NmbLinkTarget? = null,
    val hiddenGroupId: Int? = null,
    val inlinePreviewGroupId: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isSmall: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isHidden: Boolean = false,
    val isCode: Boolean = false,
    val useLabelMediumStyle: Boolean = false,
    val suppressLinkUnderline: Boolean = false,
    val useInternalLinkColorOverride: Boolean = false
)

enum class NmbRichTextSemanticColor {
    ON_SURFACE,
    ON_SURFACE_VARIANT,
    OUTLINE
}

private data class NmbRichTextScope(
    val tagName: String,
    val color: String? = null,
    val href: String? = null,
    val hiddenGroupId: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isSmall: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isHidden: Boolean = false,
    val isCode: Boolean = false
)

private data class NmbRichTextStyle(
    val color: String? = null,
    val href: String? = null,
    val hiddenGroupId: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isSmall: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isHidden: Boolean = false,
    val isCode: Boolean = false
)

private data class NmbRichListScope(
    val ordered: Boolean,
    val nextIndex: Int = 1
)

private val htmlTagRegex = Regex("<[^>]+>")
private val hiddenTextRegex = Regex("\\[h]([\\s\\S]*?)\\[/h]", RegexOption.IGNORE_CASE)
private val sourceLineBreakRegex = Regex("\\r\\n?|\\n")
private val quotedThreadIdRegex = Regex("(?:>>|>)(?!>)\\s*\\d{8}(?![0-9A-Za-z./])")
private val threadReferenceRegex = Regex(
    "(?:>>\\s*(?:No\\.\\s*)?\\d+)|(?:No\\.\\s*\\d{5,})",
    RegexOption.IGNORE_CASE
)
private val rawUrlRegex = Regex("https?://[^\\s<]+", RegexOption.IGNORE_CASE)
private val htmlEntityRegex = Regex("&#(\\d+);")
private val tagAttributeRegex =
    Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))")
private val styleColorRegex = Regex("(?i)(?:^|;)\\s*color\\s*:\\s*([^;]+)")

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

private val namedHtmlColors = mapOf(
    "deepskyblue" to "#00BFFF"
)

fun parseNmbRichText(html: String?): NmbRichText {
    if (html.isNullOrBlank()) {
        return NmbRichText(emptyList())
    }

    var hiddenGroupId = 0
    val normalizedHtml = html.replace(hiddenTextRegex) { matchResult ->
        buildString {
            append("<span class=\"h-hidden-text\" data-hidden-group-id=\"")
            append(hiddenGroupId++)
            append("\">")
            append(matchResult.groupValues[1])
            append("</span>")
        }
    }
    val scopes = mutableListOf<NmbRichTextScope>()
    val listScopes = mutableListOf<NmbRichListScope>()
    val segments = mutableListOf<NmbRichTextSegment>()
    var cursor = 0

    htmlTagRegex.findAll(normalizedHtml).forEach { match ->
        appendNmbRichText(
            rawText = normalizedHtml.substring(cursor, match.range.first),
            scopes = scopes,
            segments = segments
        )
        handleNmbRichTag(
            rawTag = match.value,
            scopes = scopes,
            listScopes = listScopes,
            segments = segments
        )
        cursor = match.range.last + 1
    }

    appendNmbRichText(
        rawText = normalizedHtml.substring(cursor),
        scopes = scopes,
        segments = segments
    )

    return NmbRichText(
        segments = normalizeNmbRichWhitespace(
            mergeNmbRichSegments(segments)
        )
    )
}

fun htmlToPlainText(html: String?): String {
    return parseNmbRichText(html).plainText
}

fun shouldRenderNmbRichText(html: String?): Boolean {
    val source = html?.takeIf { value -> value.isNotBlank() } ?: return false
    return '<' in source || source.contains("[h]", ignoreCase = true) || source.contains("[/h]", ignoreCase = true)
}

private fun appendNmbRichText(
    rawText: String,
    scopes: List<NmbRichTextScope>,
    segments: MutableList<NmbRichTextSegment>
) {
    if (rawText.isEmpty()) {
        return
    }

    val style = resolveNmbRichStyle(scopes)
    val normalizedText = decodeNmbHtmlEntities(rawText)
        .let { decoded ->
            if (style.isCode) {
                decoded
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
            } else {
                decoded.replace(sourceLineBreakRegex, "")
            }
        }

    if (normalizedText.isEmpty()) {
        return
    }

    if (style.href != null) {
        segments += NmbRichTextSegment(
            text = normalizedText,
            color = style.color,
            linkTarget = resolveNmbUrlLinkTarget(style.href),
            hiddenGroupId = style.hiddenGroupId,
            isBold = style.isBold,
            isItalic = style.isItalic,
            isSmall = style.isSmall,
            isUnderline = style.isUnderline,
            isStrikethrough = style.isStrikethrough,
            isHidden = style.isHidden,
            isCode = style.isCode
        )
        return
    }

    appendDetectedNmbRichSegments(
        text = normalizedText,
        style = style,
        segments = segments
    )
}

private fun appendDetectedNmbRichSegments(
    text: String,
    style: NmbRichTextStyle,
    segments: MutableList<NmbRichTextSegment>
) {
    var cursor = 0

    while (cursor < text.length) {
        val nextQuotedThreadId = if (style.isSmall) {
            null
        } else {
            quotedThreadIdRegex.find(text, cursor)
        }
        val nextThreadReference = findNextPostReferenceMatch(text, cursor)
        val nextRawUrl = rawUrlRegex.find(text, cursor)
        val nextMatch = listOfNotNull(nextQuotedThreadId, nextThreadReference, nextRawUrl)
            .minByOrNull { it.range.first }

        if (nextMatch == null) {
            appendResolvedNmbSegment(
                segments = segments,
                text = text.substring(cursor),
                style = style
            )
            return
        }

        if (nextMatch.range.first > cursor) {
            appendResolvedNmbSegment(
                segments = segments,
                text = text.substring(cursor, nextMatch.range.first),
                style = style
            )
        }

        val matchedText = nextMatch.value
        val linkTarget = when (nextMatch) {
            nextQuotedThreadId ->
                parseNmbQuotedThreadIdInput(matchedText)?.let { threadId ->
                    NmbLinkTarget.Thread(threadId = threadId)
                }
            nextThreadReference -> parseNmbPostReference(matchedText)?.let(NmbLinkTarget::PostReference)
            nextRawUrl -> resolveNmbUrlLinkTarget(matchedText)
            else -> null
        }
        appendResolvedNmbSegment(
            segments = segments,
            text = matchedText,
            style = style,
            linkTarget = linkTarget
        )
        cursor = nextMatch.range.last + 1
    }
}

private fun findNextPostReferenceMatch(text: String, startIndex: Int): MatchResult? {
    var match = threadReferenceRegex.find(text, startIndex)
    while (match != null && parseNmbQuotedThreadIdInput(match.value) != null) {
        val nextStartIndex = match.range.first + 1
        match = if (nextStartIndex < text.length) {
            threadReferenceRegex.find(text, nextStartIndex)
        } else {
            null
        }
    }
    return match
}

private fun appendResolvedNmbSegment(
    segments: MutableList<NmbRichTextSegment>,
    text: String,
    style: NmbRichTextStyle,
    linkTarget: NmbLinkTarget? = style.href?.let(::resolveNmbUrlLinkTarget)
) {
    if (text.isEmpty()) {
        return
    }

    segments += NmbRichTextSegment(
        text = text,
        color = style.color,
        linkTarget = linkTarget,
        hiddenGroupId = style.hiddenGroupId,
        isBold = style.isBold,
        isItalic = style.isItalic,
        isSmall = style.isSmall,
        isUnderline = style.isUnderline,
        isStrikethrough = style.isStrikethrough,
        isHidden = style.isHidden,
        isCode = style.isCode
    )
}

private fun handleNmbRichTag(
    rawTag: String,
    scopes: MutableList<NmbRichTextScope>,
    listScopes: MutableList<NmbRichListScope>,
    segments: MutableList<NmbRichTextSegment>
) {
    val normalizedTag = rawTag
        .removePrefix("<")
        .removeSuffix(">")
        .trim()

    if (normalizedTag.startsWith("!--")) {
        return
    }

    val isClosingTag = normalizedTag.startsWith("/")
    val tagBody = normalizedTag
        .removePrefix("/")
        .removeSuffix("/")
        .trim()
    val tagName = tagBody.substringBefore(' ').lowercase()
    val currentStyle = resolveNmbRichStyle(scopes)

    if (isClosingTag) {
        when (tagName) {
            "ul", "ol" -> {
                if (listScopes.isNotEmpty()) {
                    listScopes.removeAt(listScopes.lastIndex)
                }
            }
            "p" -> appendResolvedNmbSegment(
                segments = segments,
                text = "\n\n",
                style = currentStyle
            )
            "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6" ->
                appendResolvedNmbSegment(
                    segments = segments,
                    text = "\n",
                    style = currentStyle
                )
            else -> popNmbRichScope(scopes, tagName)
        }
        return
    }

    when (tagName) {
        "br" -> appendResolvedNmbSegment(
            segments = segments,
            text = "\n",
            style = currentStyle
        )
        "ul" -> listScopes += NmbRichListScope(ordered = false)
        "ol" -> listScopes += NmbRichListScope(ordered = true)
        "li" -> appendResolvedNmbSegment(
            segments = segments,
            text = listScopes.nextListMarker(),
            style = currentStyle
        )
        "font" -> {
            parseTagAttributes(tagBody)["color"]?.let(::normalizeNamedHtmlColor)?.let { color ->
                scopes += NmbRichTextScope(
                    tagName = tagName,
                    color = color
                )
            }
        }
        "span" -> {
            val attributes = parseTagAttributes(tagBody)
            val color = attributes["style"]
                ?.let(styleColorRegex::find)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { value -> value.isNotEmpty() }
                ?.let(::normalizeNamedHtmlColor)
            val hiddenGroupId = attributes["data-hidden-group-id"]?.toIntOrNull()
            val isHidden = attributes["class"]
                ?.split(' ')
                ?.any { token -> token.equals("h-hidden-text", ignoreCase = true) }
                ?: false

            if (color != null || isHidden) {
                scopes += NmbRichTextScope(
                    tagName = tagName,
                    color = color,
                    hiddenGroupId = hiddenGroupId,
                    isHidden = isHidden
                )
            }
        }
        "a" -> {
            parseTagAttributes(tagBody)["href"]?.takeIf { value -> value.isNotBlank() }?.let { href ->
                scopes += NmbRichTextScope(
                    tagName = tagName,
                    href = href
                )
            }
        }
        "b", "strong" -> scopes += NmbRichTextScope(tagName = tagName, isBold = true)
        "i", "em" -> scopes += NmbRichTextScope(tagName = tagName, isItalic = true)
        "small" -> scopes += NmbRichTextScope(tagName = tagName, isSmall = true)
        "u" -> scopes += NmbRichTextScope(tagName = tagName, isUnderline = true)
        "s", "strike", "del" -> scopes += NmbRichTextScope(tagName = tagName, isStrikethrough = true)
        "pre" -> scopes += NmbRichTextScope(tagName = tagName, isCode = true)
    }
}

private fun MutableList<NmbRichListScope>.nextListMarker(): String {
    val scope = lastOrNull() ?: return "• "
    if (!scope.ordered) {
        return "• "
    }
    this[lastIndex] = scope.copy(nextIndex = scope.nextIndex + 1)
    return "${scope.nextIndex}. "
}

private fun resolveNmbRichStyle(scopes: List<NmbRichTextScope>): NmbRichTextStyle {
    var color: String? = null
    var href: String? = null
    for (index in scopes.indices.reversed()) {
        val scope = scopes[index]
        if (color == null && scope.color != null) {
            color = scope.color
        }
        if (href == null && scope.href != null) {
            href = scope.href
        }
        if (color != null && href != null) {
            break
        }
    }

    return NmbRichTextStyle(
        color = color,
        href = href,
        hiddenGroupId = scopes.lastOrNull { scope -> scope.hiddenGroupId != null }?.hiddenGroupId,
        isBold = scopes.any { scope -> scope.isBold },
        isItalic = scopes.any { scope -> scope.isItalic },
        isSmall = scopes.any { scope -> scope.isSmall },
        isUnderline = scopes.any { scope -> scope.isUnderline },
        isStrikethrough = scopes.any { scope -> scope.isStrikethrough },
        isHidden = scopes.any { scope -> scope.isHidden },
        isCode = scopes.any { scope -> scope.isCode }
    )
}

private fun popNmbRichScope(
    scopes: MutableList<NmbRichTextScope>,
    tagName: String
) {
    val index = scopes.indexOfLast { scope -> scope.tagName == tagName }
    if (index >= 0) {
        scopes.removeAt(index)
    }
}

private fun parseTagAttributes(tagBody: String): Map<String, String> {
    return buildMap {
        tagAttributeRegex.findAll(tagBody).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues.drop(2)
                .firstOrNull { group -> group.isNotEmpty() }
                ?.let(::decodeNmbHtmlEntities)
                ?: return@forEach
            put(key, value)
        }
    }
}

private fun normalizeNamedHtmlColor(rawColor: String): String {
    val trimmed = rawColor.trim()
    return namedHtmlColors[trimmed.lowercase()] ?: trimmed
}

private fun decodeNmbHtmlEntities(text: String): String {
    val decodedNamedEntities = htmlEntities.entries.fold(text) { acc, (entity, replacement) ->
        acc.replace(entity, replacement)
    }

    return htmlEntityRegex.replace(decodedNamedEntities) { match ->
        match.groupValues.getOrNull(1)
            ?.toIntOrNull()
            ?.toChar()
            ?.toString()
            ?: match.value
    }
}

private fun mergeNmbRichSegments(
    segments: List<NmbRichTextSegment>
): List<NmbRichTextSegment> {
    if (segments.isEmpty()) {
        return emptyList()
    }

    val merged = mutableListOf<NmbRichTextSegment>()
    segments.forEach { segment ->
        val previous = merged.lastOrNull()
        if (previous != null &&
            '\n' !in previous.text &&
            '\n' !in segment.text &&
            previous.color == segment.color &&
            previous.semanticColor == segment.semanticColor &&
            previous.linkTarget == segment.linkTarget &&
            previous.hiddenGroupId == segment.hiddenGroupId &&
            previous.inlinePreviewGroupId == segment.inlinePreviewGroupId &&
            previous.isBold == segment.isBold &&
            previous.isItalic == segment.isItalic &&
            previous.isSmall == segment.isSmall &&
            previous.isUnderline == segment.isUnderline &&
            previous.isStrikethrough == segment.isStrikethrough &&
            previous.isHidden == segment.isHidden &&
            previous.isCode == segment.isCode &&
            previous.useLabelMediumStyle == segment.useLabelMediumStyle &&
            previous.suppressLinkUnderline == segment.suppressLinkUnderline &&
            previous.useInternalLinkColorOverride == segment.useInternalLinkColorOverride
        ) {
            merged[merged.lastIndex] = previous.copy(
                text = previous.text + segment.text
            )
        } else {
            merged += segment
        }
    }
    return merged
}

private fun normalizeNmbRichWhitespace(
    segments: List<NmbRichTextSegment>
): List<NmbRichTextSegment> {
    return mergeNmbRichSegments(
        segments.filterNot { segment -> segment.text.isEmpty() }
    )
}
