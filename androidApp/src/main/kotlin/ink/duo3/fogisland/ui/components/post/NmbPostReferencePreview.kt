package ink.duo3.fogisland.ui.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.collectNmbPostReferenceCandidates
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.linkifyNmbPureNumericReferences
import ink.duo3.fogisland.shared.util.parseNmbRichText
import ink.duo3.fogisland.shared.util.splitNmbRichTextLinesPreservingBreaks
import ink.duo3.fogisland.ui.components.preview.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.preview.NmbPreviewSamples
import ink.duo3.fogisland.ui.components.richtext.NmbRichTextText
import ink.duo3.fogisland.utils.ProvideContentColorTextStyle

data class NmbPostReferencePreviewState(
    val resolvedPosts: Map<Long, NmbPost> = emptyMap(),
    val onReferenceObserved: ((Long) -> Unit)? = null,
    val onPostReferenceClick: ((Long, Long) -> Unit)? = null,
    val onImageClick: ((String, String?) -> Unit)? = null
)

@Composable
fun NmbReferencedPostBody(
    sourcePostId: Long,
    html: String?,
    fallbackText: String?,
    style: TextStyle,
    color: Color,
    referencePreviewState: NmbPostReferencePreviewState,
    modifier: Modifier = Modifier,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val source = html?.takeIf { it.isNotBlank() } ?: fallbackText.orEmpty()
    val parsedRichText = remember(source) { parseNmbRichText(source) }
    val observedCandidates = remember(parsedRichText) {
        collectNmbPostReferenceCandidates(parsedRichText)
    }
    val resolvedPosts = referencePreviewState.resolvedPosts
    val linkifiedRichText = remember(parsedRichText, resolvedPosts) {
        linkifyNmbPureNumericReferences(parsedRichText) { postId, rawText ->
            rawText.length == 8 || postId in resolvedPosts
        }
    }
    val lines = remember(linkifiedRichText) {
        splitNmbRichTextLinesPreservingBreaks(linkifiedRichText)
    }
    val previewBlocks = remember(lines, resolvedPosts) {
        buildReferencePreviewBlocks(lines, resolvedPosts)
    }

    LaunchedEffect(observedCandidates, referencePreviewState.onReferenceObserved) {
        observedCandidates.forEach { postId ->
            referencePreviewState.onReferenceObserved?.invoke(postId)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        previewBlocks.forEach { block ->
            when (block) {
                is ReferencePreviewBlock.StandaloneCard -> {
                    StandalonePostReferencePreviewCard(
                        referenceText = block.referenceText,
                        post = block.post,
                        resolvedPosts = resolvedPosts,
                        onImageClick = referencePreviewState.onImageClick,
                        onClick = {
                            referencePreviewState.onPostReferenceClick?.invoke(
                                sourcePostId,
                                block.referenceId
                            )
                        }
                    )
                }

                is ReferencePreviewBlock.RichTextBody -> {
                    NmbRichTextText(
                        richText = block.richText.appendInlineReferenceSnippets(resolvedPosts),
                        style = style,
                        color = color,
                        interactionsEnabled = true,
                        onLinkClick = { target ->
                            when (target) {
                                is NmbLinkTarget.PostReference ->
                                    referencePreviewState.onPostReferenceClick?.invoke(
                                        sourcePostId,
                                        target.postId
                                    )
                                else -> onLinkClick?.invoke(target)
                            }
                        },
                        maxLines = maxLines,
                        overflow = overflow
                    )
                }
            }
        }
    }
}

@Composable
private fun StandalonePostReferencePreviewCard(
    referenceText: String,
    post: NmbPost,
    resolvedPosts: Map<Long, NmbPost>,
    onImageClick: ((String, String?) -> Unit)?,
    onClick: () -> Unit
) {
    val postedAtText = rememberStandaloneReferencePostedAtText(post.postedAtEpochMillis)
    val bodyRichText = remember(post.contentHtml, post.contentText, resolvedPosts) {
        val source = post.contentHtml.takeIf { it.isNotBlank() }
            ?: buildStandalonePostReferenceFallbackText(post)
        parseNmbRichText(source).appendCardPreviewReferenceStyling(resolvedPosts)
    }
    val postImage = post.image
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 8.dp, 12.dp, 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProvideContentColorTextStyle(
                    contentColor = MaterialTheme.colorScheme.outline,
                    textStyle = MaterialTheme.typography.labelMedium
                ) {
                    Text(
                        text = referenceText,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        " · "
                    )
                    Text(
                        text = post.userHash,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (post.isPoster) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (post.isPoster) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                    if (post.isPoster) {
                        Text(
                            text = " · PO",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    postedAtText?.let { displayPostedAtText ->
                        Text(
                            modifier = Modifier.weight(1f),
                            text = displayPostedAtText,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            NmbRichTextText(
                richText = bodyRichText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                linkColor = MaterialTheme.colorScheme.onSurface,
                referenceColor = MaterialTheme.colorScheme.onSurface,
                internalLinkColorOverride = MaterialTheme.colorScheme.onSurface,
                interactionsEnabled = false,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!postImage.isNullOrBlank()) {
                ThreadImagePreview(
                    image = postImage,
                    ext = post.ext,
                    onImageClick = onImageClick ?: { _, _ -> },
                    previewHeight = ReferencePreviewImageHeight,
                    minimumPreviewWidth = ReferencePreviewImageMinimumWidth,
                    cornerRadius = 6.dp
                )
            }
        }
    }
}

@Composable
private fun rememberStandaloneReferencePostedAtText(epochMillis: Long?): String? {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    return formatNmbPostedAtText(
        epochMillis = epochMillis,
        options = timeFormatOptions
    )
}

private val ReferencePreviewImageHeight = 64.dp
private val ReferencePreviewImageMinimumWidth = 48.dp

private val PreviewReferenceTargetPrimary = NmbPreviewSamples.replyPost.copy(
    id = 45678961L,
    remoteId = 45678961L,
    contentHtml = "<font color=\"#789922\">&gt;&gt;No.45670000</font><br />这是一条被引用的回复，用来看独立预览卡片的密度、圆角和标题样式。",
    contentText = ">>No.45670000 这是一条被引用的回复，用来看独立预览卡片的密度、圆角和标题样式。",
    image = NmbPreviewSamples.forumThread.image,
    ext = NmbPreviewSamples.forumThread.ext
)

private val PreviewReferenceTargetSecondary = NmbPreviewSamples.replyPost.copy(
    id = 45678962L,
    remoteId = 45678962L,
    contentHtml = "另一条被引用回复，正文稍短一点，用来看 inline 摘要长度。",
    contentText = "另一条被引用回复，正文稍短一点，用来看 inline 摘要长度。",
    image = null,
    ext = null
)

private val PreviewStandaloneReferenceSource = NmbPreviewSamples.replyPost.copy(
    id = 45678971L,
    remoteId = 45678971L,
    contentHtml = "<font color=\"#789922\">&gt;&gt;No.45678961</font>",
    contentText = ">>No.45678961",
    image = null,
    ext = null
)

private val PreviewInlineReferenceSource = NmbPreviewSamples.replyPost.copy(
    id = 45678972L,
    remoteId = 45678972L,
    contentHtml = "我觉得<font color=\"#789922\">&gt;&gt;No.45678961</font>说得对，另外<font color=\"#789922\">&gt;&gt;45678962</font>也可以一起看。",
    contentText = "我觉得>>No.45678961说得对，另外>>45678962也可以一起看。",
    image = null,
    ext = null
)

private val PreviewMixedReferenceSource = NmbPreviewSamples.replyPost.copy(
    id = 45678973L,
    remoteId = 45678973L,
    contentHtml = buildString {
        append("开头先说一句正文。<br />")
        append("<font color=\"#789922\">&gt;&gt;No.45678961</font><br />")
        append("中间再接一段带 inline 引用的正文 <font color=\"#789922\">&gt;&gt;45678962</font>，用来看段间距。<br />")
        append("<font color=\"#789922\">&gt;&gt;No.45678962</font>")
    },
    contentText = """
        开头先说一句正文。
        >>No.45678961
        中间再接一段带 inline 引用的正文 >>45678962，用来看段间距。
        >>No.45678962
    """.trimIndent(),
    image = null,
    ext = null
)

private val PreviewReferenceState = NmbPostReferencePreviewState(
    resolvedPosts = mapOf(
        PreviewReferenceTargetPrimary.remoteId to PreviewReferenceTargetPrimary,
        PreviewReferenceTargetSecondary.remoteId to PreviewReferenceTargetSecondary
    )
)

@Preview(name = "Reference Card", widthDp = 412, heightDp = 220)
@Composable
private fun StandalonePostReferencePreviewCardPreview() {
    FogIslandPreviewColumn {
        StandalonePostReferencePreviewCard(
            referenceText = ">>No.${PreviewReferenceTargetPrimary.remoteId}",
            post = PreviewReferenceTargetPrimary,
            resolvedPosts = PreviewReferenceState.resolvedPosts,
            onImageClick = { _, _ -> },
            onClick = {}
        )
    }
}

@Preview(name = "Inline Reference Body", widthDp = 412, heightDp = 220)
@Composable
private fun InlinePostReferencePreviewBodyPreview() {
    FogIslandPreviewColumn {
        NmbReferencedPostBody(
            sourcePostId = PreviewInlineReferenceSource.remoteId,
            html = PreviewInlineReferenceSource.contentHtml,
            fallbackText = PreviewInlineReferenceSource.contentText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            referencePreviewState = PreviewReferenceState,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Mixed Reference Body", widthDp = 412, heightDp = 420)
@Composable
private fun MixedPostReferencePreviewBodyPreview() {
    FogIslandPreviewColumn {
        NmbReferencedPostBody(
            sourcePostId = PreviewMixedReferenceSource.remoteId,
            html = PreviewMixedReferenceSource.contentHtml,
            fallbackText = PreviewMixedReferenceSource.contentText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            referencePreviewState = PreviewReferenceState,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip
        )
    }
}
