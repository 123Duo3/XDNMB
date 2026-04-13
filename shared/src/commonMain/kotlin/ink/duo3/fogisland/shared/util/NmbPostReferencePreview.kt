package ink.duo3.fogisland.shared.util

private val pureNumericReferenceRegex = Regex("\\d+")

data class NmbRichTextLine(
    val richText: NmbRichText,
    val trailingBreak: NmbRichTextSegment? = null
)

fun collectNmbPostReferenceCandidates(richText: NmbRichText): Set<Long> {
    if (richText.segments.isEmpty()) {
        return emptySet()
    }

    val candidates = linkedSetOf<Long>()
    richText.segments.forEach { segment ->
        (segment.linkTarget as? NmbLinkTarget.PostReference)?.postId?.let(candidates::add)
        if (segment.linkTarget != null || segment.isCode || segment.isSmall) {
            return@forEach
        }

        collectPureNumericReferenceCandidates(segment.text).forEach { candidate ->
            candidates += candidate
        }
    }
    return candidates
}

fun linkifyNmbPureNumericReferences(
    richText: NmbRichText,
    shouldLink: (postId: Long, rawText: String) -> Boolean
): NmbRichText {
    if (richText.segments.isEmpty()) {
        return richText
    }

    val resolved = mutableListOf<NmbRichTextSegment>()
    richText.segments.forEach { segment ->
        if (segment.linkTarget != null || segment.isCode || segment.isSmall) {
            resolved += segment
            return@forEach
        }

        val text = segment.text
        var cursor = 0
        var appendedAnyLink = false
        pureNumericReferenceRegex.findAll(text).forEach { match ->
            val candidateText = match.value
            val postId = candidateText.toLongOrNull() ?: return@forEach
            if (!isPureNumericReferenceBoundary(text, match.range.first, match.range.last + 1)) {
                return@forEach
            }
            if (!shouldLink(postId, candidateText)) {
                return@forEach
            }

            if (match.range.first > cursor) {
                resolved += segment.copy(
                    text = text.substring(cursor, match.range.first)
                )
            }
            resolved += segment.copy(
                text = candidateText,
                linkTarget = NmbLinkTarget.PostReference(postId)
            )
            cursor = match.range.last + 1
            appendedAnyLink = true
        }

        if (!appendedAnyLink) {
            resolved += segment
            return@forEach
        }

        if (cursor < text.length) {
            resolved += segment.copy(
                text = text.substring(cursor)
            )
        }
    }

    return NmbRichText(
        segments = resolved.filterNot { it.text.isEmpty() }
    )
}

fun splitNmbRichTextLines(richText: NmbRichText): List<NmbRichText> {
    return splitNmbRichTextLinesPreservingBreaks(richText).map(NmbRichTextLine::richText)
}

fun splitNmbRichTextLinesPreservingBreaks(richText: NmbRichText): List<NmbRichTextLine> {
    if (richText.segments.isEmpty()) {
        return listOf(NmbRichTextLine(richText = NmbRichText(emptyList())))
    }

    val lineSegments = mutableListOf(mutableListOf<NmbRichTextSegment>())
    val trailingBreaks = mutableListOf<NmbRichTextSegment?>(null)

    richText.segments.forEach { segment ->
        var cursor = 0
        segment.text.forEachIndexed { index, char ->
            if (char != '\n') {
                return@forEachIndexed
            }

            if (index > cursor) {
                lineSegments.last() += segment.copy(
                    text = segment.text.substring(cursor, index)
                )
            }
            trailingBreaks[trailingBreaks.lastIndex] = segment.copy(text = "\n")
            lineSegments.add(mutableListOf())
            trailingBreaks += null
            cursor = index + 1
        }

        if (cursor < segment.text.length) {
            lineSegments.last() += segment.copy(
                text = segment.text.substring(cursor)
            )
        }
    }

    return lineSegments.indices.map { index ->
        NmbRichTextLine(
            richText = NmbRichText(
                lineSegments[index].filterNot { it.text.isEmpty() }
            ),
            trailingBreak = trailingBreaks[index]
        )
    }
}

fun joinNmbRichTextLines(lines: List<NmbRichTextLine>): NmbRichText {
    if (lines.isEmpty()) {
        return NmbRichText(emptyList())
    }

    val segments = buildList {
        lines.forEach { line ->
            addAll(line.richText.segments)
            line.trailingBreak?.let(::add)
        }
    }.filterNot { it.text.isEmpty() }

    return NmbRichText(segments)
}

fun findStandaloneNmbPostReferenceId(richText: NmbRichText): Long? {
    val plainText = richText.plainText.trimAsciiEdgeWhitespace()
    if (plainText.isEmpty()) {
        return null
    }

    val nonWhitespaceSegments = richText.segments.filter { segment ->
        segment.text.any { char -> !char.isWhitespace() }
    }
    if (nonWhitespaceSegments.size != 1) {
        return null
    }

    val referenceSegment = nonWhitespaceSegments.single()
    val referenceId = (referenceSegment.linkTarget as? NmbLinkTarget.PostReference)?.postId ?: return null
    return referenceId.takeIf { referenceSegment.text.trimAsciiEdgeWhitespace() == plainText }
}

private fun collectPureNumericReferenceCandidates(text: String): List<Long> {
    return pureNumericReferenceRegex.findAll(text)
        .filter { match ->
            isPureNumericReferenceBoundary(
                text = text,
                start = match.range.first,
                endExclusive = match.range.last + 1
            )
        }
        .mapNotNull { match -> match.value.toLongOrNull() }
        .toList()
}

private fun isPureNumericReferenceBoundary(
    text: String,
    start: Int,
    endExclusive: Int
): Boolean {
    val previous = text.getOrNull(start - 1)
    val next = text.getOrNull(endExclusive)
    return !isBlockedNumericReferenceNeighbor(previous) && !isBlockedNumericReferenceNeighbor(next)
}

private fun isBlockedNumericReferenceNeighbor(char: Char?): Boolean {
    val value = char ?: return false
    return value.isAsciiWordChar() || value == '.' || value == '/'
}

private fun Char.isAsciiWordChar(): Boolean {
    return isDigit() || this in 'a'..'z' || this in 'A'..'Z'
}

private fun String.trimAsciiEdgeWhitespace(): String {
    return trim { char -> char == ' ' || char == '\t' || char == '\r' || char == '\n' }
}
