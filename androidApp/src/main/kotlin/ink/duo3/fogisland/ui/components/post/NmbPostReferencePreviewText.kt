package ink.duo3.fogisland.ui.components.post

import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.NmbRichText
import ink.duo3.fogisland.shared.util.NmbRichTextLine
import ink.duo3.fogisland.shared.util.NmbRichTextSegment
import ink.duo3.fogisland.shared.util.findStandaloneNmbPostReferenceId
import ink.duo3.fogisland.shared.util.joinNmbRichTextLines

internal sealed interface ReferencePreviewBlock {
    data class RichTextBody(val richText: NmbRichText) : ReferencePreviewBlock
    data class StandaloneCard(
        val referenceId: Long,
        val referenceText: String,
        val post: NmbPost
    ) : ReferencePreviewBlock
}

internal fun buildReferencePreviewBlocks(
    lines: List<NmbRichTextLine>,
    resolvedPosts: Map<Long, NmbPost>
): List<ReferencePreviewBlock> {
    val blocks = mutableListOf<ReferencePreviewBlock>()
    val pendingBodyLines = mutableListOf<NmbRichTextLine>()

    fun flushBodyLines() {
        if (pendingBodyLines.isEmpty()) {
            return
        }
        blocks += ReferencePreviewBlock.RichTextBody(
            joinNmbRichTextLines(pendingBodyLines)
        )
        pendingBodyLines.clear()
    }

    lines.forEach { line ->
        val standaloneReferenceId = findStandaloneNmbPostReferenceId(line.richText)
        val standalonePost = standaloneReferenceId?.let(resolvedPosts::get)
        if (standaloneReferenceId != null && standalonePost != null) {
            flushBodyLines()
            blocks += ReferencePreviewBlock.StandaloneCard(
                referenceId = standaloneReferenceId,
                referenceText = line.richText.plainText.trim(),
                post = standalonePost
            )
        } else {
            pendingBodyLines += line
        }
    }

    flushBodyLines()
    return blocks
}

internal fun NmbRichText.appendInlineReferenceSnippets(
    resolvedPosts: Map<Long, NmbPost>
): NmbRichText {
    if (segments.isEmpty()) {
        return this
    }

    val appended = mutableListOf<NmbRichTextSegment>()
    segments.forEachIndexed { index, segment ->
        val postId = (segment.linkTarget as? NmbLinkTarget.PostReference)?.postId
        val post = postId?.let(resolvedPosts::get)
        val inlinePreviewGroupId = post?.let { index + 1 }
        val needsLeadingSpacer = appended.lastOrNull()?.text?.lastOrNull()?.isWhitespace() != true
        val needsTrailingSpacer = segments
            .getOrNull(index + 1)
            ?.text
            ?.firstOrNull()
            ?.isWhitespace() != true
        if (post != null && needsLeadingSpacer) {
            appended += NmbRichTextSegment(text = "  ")
        }
        appended += if (post != null) {
            segment.copy(
                text = " ${segment.text}",
                inlinePreviewGroupId = inlinePreviewGroupId,
                useLabelMediumStyle = true,
                suppressLinkUnderline = true
            )
        } else {
            segment
        }
        if (post == null) {
            return@forEachIndexed
        }
        val snippet = buildInlinePostReferenceSummary(post, maxLength = 18)
        if (snippet.isBlank()) {
            return@forEachIndexed
        }
        appended += NmbRichTextSegment(
            text = " $snippet ",
            inlinePreviewGroupId = inlinePreviewGroupId,
            useLabelMediumStyle = true
        )
        if (needsTrailingSpacer) {
            appended += NmbRichTextSegment(text = "  ")
        }
    }
    return NmbRichText(appended)
}

internal fun NmbRichText.appendCardPreviewReferenceStyling(
    resolvedPosts: Map<Long, NmbPost>
): NmbRichText {
    if (segments.isEmpty()) {
        return this
    }

    return NmbRichText(
        segments = segments.map { segment ->
            val postId = (segment.linkTarget as? NmbLinkTarget.PostReference)?.postId
            if (postId != null && postId in resolvedPosts) {
                segment.copy(
                    suppressLinkUnderline = true,
                    useInternalLinkColorOverride = true
                )
            } else {
                segment
            }
        }
    )
}

internal fun buildStandalonePostReferenceFallbackText(
    post: NmbPost
): String = when {
    post.contentText.isNotBlank() -> post.contentText
    !post.title.isNullOrBlank() -> post.title.orEmpty()
    !post.image.isNullOrBlank() -> "[图片回复]"
    else -> ""
}
    .replace("\r\n", "\n")
    .lines()
    .joinToString("\n") { line ->
        line.replace(Regex("[\\t ]+"), " ").trim()
    }
    .trim()

private fun buildInlinePostReferenceSummary(
    post: NmbPost,
    maxLength: Int
): String {
    val normalized = when {
        post.contentText.isNotBlank() -> post.contentText
        !post.title.isNullOrBlank() -> post.title.orEmpty()
        !post.image.isNullOrBlank() -> "[图片回复]"
        else -> ""
    }.trim().replace(Regex("\\s+"), " ")

    if (normalized.isBlank()) {
        return ""
    }

    return if (normalized.length > maxLength) {
        normalized.take(maxLength).trimEnd() + "…"
    } else {
        normalized
    }
}
