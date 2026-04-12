package ink.duo3.fogisland.ui.components.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.R
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SearchHitType
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.shouldRenderNmbRichText
import ink.duo3.fogisland.ui.components.preview.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.preview.NmbPreviewSamples
import ink.duo3.fogisland.ui.components.richtext.NmbRichTextText
import ink.duo3.fogisland.utils.ProvideContentColorTextStyle

private val NmbPostCardShape = RoundedCornerShape(24.dp)
private val NmbPostCardPadding = PaddingValues(16.dp, 12.dp)
private val NmbPostFlatItemPadding = PaddingValues(16.dp, 12.dp)

@Composable
fun NmbPostCard(
    post: NmbPost,
    forumName: String?,
    onClick: () -> Unit,
    onImageClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    bodyMaxLines: Int = Int.MAX_VALUE,
    bodyOverflow: TextOverflow = TextOverflow.Clip
) {
    NmbPostCardLayout(
        onClick = onClick,
        modifier = modifier.padding(16.dp, 8.dp),
        onLongClick = onLongClick
    ) {
        val postedAtText = rememberNmbPostedAtText(post.postedAtEpochMillis)
        NmbPostHeader(
            userHash = post.userHash,
            isAdmin = post.admin,
            postedAtText = postedAtText,
            isSage = post.sage,
        )
        NmbPostContent(
            sourcePostId = post.remoteId,
            title = post.title,
            subtitle = post.name,
            bodyHtml = post.contentHtml,
            fallbackBodyText = post.contentText,
            bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
            bodyInteractionsEnabled = false,
            bodyMaxLines = bodyMaxLines,
            bodyOverflow = bodyOverflow,
            image = post.image,
            ext = post.ext,
            onImageClick = onImageClick
        )
        NmbPostFooter(
            contextText = buildNmbContextText(forumName, post.remoteId),
            replyCount = post.replyCount
        )
    }
}

@Composable
fun NmbPostCard(
    hit: SearchHit,
    query: String,
    forumName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bodyMaxLines: Int = 6,
    bodyOverflow: TextOverflow = TextOverflow.Ellipsis
) {
    NmbPostCardLayout(
        onClick = onClick,
        modifier = modifier,
        onLongClick = null
    ) {
        val postedAtText = rememberNmbPostedAtText(hit.postedAtEpochMillis)
        NmbPostHeader(
            userHash = hit.userHash,
            isAdmin = hit.admin,
            postedAtText = postedAtText,
            isSage = hit.sage,
        )
        NmbPostContent(
            title = hit.title
                ?: when (hit.type) {
                    SearchHitType.THREAD -> null
                    SearchHitType.POST -> "回复 No.${hit.postId}"
                },
            subtitle = hit.name,
            customBody = hit.preview
                .takeIf { it.isNotBlank() }
                ?.let { previewText ->
                    {
                        Text(
                            text = highlightNmbPostQuery(
                                text = previewText,
                                query = query,
                                highlightColor = MaterialTheme.colorScheme.primary
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = bodyMaxLines,
                            overflow = bodyOverflow
                        )
                    }
                },
            image = null,
            ext = null,
            onImageClick = { _, _ -> }
        )
        NmbPostFooter(
            contextText = buildNmbContextText(forumName, hit.threadId),
            replyCount = null
        )
    }
}

@Composable
fun NmbPostFlatItem(
    post: NmbPost,
    onImageClick: (String, String?) -> Unit,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    referencePreviewState: NmbPostReferencePreviewState? = null,
    modifier: Modifier = Modifier,
    showThreadId: Boolean = true,
    showDivider: Boolean = true,
    showPosterLabel: Boolean = true,
    contentPadding: PaddingValues = NmbPostFlatItemPadding,
    bodyMaxLines: Int = Int.MAX_VALUE,
    bodyOverflow: TextOverflow = TextOverflow.Clip
) {
    NmbPostFlatItemLayout(
        modifier = modifier,
        showDivider = showDivider,
        contentPadding = contentPadding
    ) {
        if(!post.isTips) {
            val postedAtText = rememberNmbPostedAtText(post.postedAtEpochMillis)
            NmbPostHeader(
                threadId = post.remoteId.takeIf { showThreadId },
                userHash = post.userHash,
                isAdmin = post.admin,
                postedAtText = postedAtText,
                isSage = post.sage,
                isPoster = post.isPoster,
                showPosterLabel = showPosterLabel
            )
        }
        NmbPostContent(
            sourcePostId = post.remoteId,
            title = post.title,
            subtitle = post.name,
            bodyHtml = post.contentHtml,
            fallbackBodyText = post.contentText,
            bodyColor = MaterialTheme.colorScheme.onSurface,
            bodyInteractionsEnabled = true,
            onLinkClick = onLinkClick,
            referencePreviewState = referencePreviewState,
            bodyMaxLines = bodyMaxLines,
            bodyOverflow = bodyOverflow,
            image = post.image,
            ext = post.ext,
            onImageClick = onImageClick,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun NmbPostCardLayout(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = NmbPostCardShape
    ) {
        NmbPostContainer(
            modifier = Modifier,
            contentPadding = NmbPostCardPadding,
            onClick = onClick,
            onLongClick = onLongClick
        )
        {
            content()
        }
    }
}

@Composable
private fun NmbPostFlatItemLayout(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    contentPadding: PaddingValues = NmbPostFlatItemPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NmbPostContainer(
            modifier = Modifier,
            contentPadding = contentPadding,
            onClick = null,
            onLongClick = null
        )
        {
            content()
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.4f
                )
            )
        }
    }
}

@Composable
private fun rememberNmbPostedAtText(epochMillis: Long?): String? {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    return formatNmbPostedAtText(
        epochMillis = epochMillis,
        options = timeFormatOptions
    )
}

@Composable
private fun NmbPostHeader(
    threadId: Long? = null,
    userHash: String,
    isAdmin: Boolean,
    postedAtText: String?,
    isSage: Boolean = false,
    isPoster: Boolean = false,
    showPosterLabel: Boolean = true
) {
    ProvideContentColorTextStyle(
        textStyle = MaterialTheme.typography.labelMedium,
        contentColor = MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            threadId?.let {
                Text(
                    text = "No.$threadId · "
                )
            }
            Text(
                text = userHash,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isPoster) {
                    FontWeight.Bold
                } else {
                    FontWeight.SemiBold
                },
                color = if (isAdmin) {
                    MaterialTheme.colorScheme.error
                } else if (isPoster) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Unspecified
                }
            )

            if (isPoster && showPosterLabel) {
                Text(
                    text = " · PO",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isSage) {
                Text(
                    text = " · ",
                    color = Color.Unspecified
                )
                Text(
                    text = "SAGE",
                    color = MaterialTheme.colorScheme.error
                )
                Icon(
                    imageVector = Icons.Default.ThumbDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp).size(12.dp)
                )
            }

            postedAtText?.let { text ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = text,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun buildNmbContextText(
    forumName: String?,
    threadId: Long? = null,
): String? {
    if (forumName.isNullOrBlank() && threadId == null) {
        return null
    }
    return buildString {
        threadId?.let { id ->
            append("No.")
            append(id)
        }
        forumName?.takeIf { it.isNotBlank() }?.let { name ->
            if (threadId != null) {
                append(" · ")
            }
            append(name)
        }
    }
}

private fun highlightNmbPostQuery(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return AnnotatedString(text)
    }

    val ranges = buildList {
        var searchStart = 0
        while (searchStart < text.length) {
            val matchIndex = text.indexOf(normalizedQuery, startIndex = searchStart, ignoreCase = true)
            if (matchIndex < 0) {
                break
            }
            add(matchIndex until (matchIndex + normalizedQuery.length))
            searchStart = matchIndex + normalizedQuery.length
        }
    }
    if (ranges.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.SemiBold
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

@Composable
private fun NmbPostContainer(
    modifier: Modifier,
    contentPadding: PaddingValues,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nmbPostInteractions(onClick = onClick, onLongClick = onLongClick)
            .padding(contentPadding),
        content = content
    )
}

@Composable
private fun NmbPostFooter(
    contextText: String?,
    replyCount: Int?
) {
    if (contextText.isNullOrBlank() && replyCount == null) {
        return
    }

    ProvideContentColorTextStyle(
        textStyle = MaterialTheme.typography.labelMedium,
        contentColor = MaterialTheme.colorScheme.outline
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!contextText.isNullOrBlank()) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = contextText
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            replyCount?.let { count ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat_bubble_12dp),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = count.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun NmbPostContent(
    modifier: Modifier = Modifier,
    sourcePostId: Long? = null,
    title: String?,
    subtitle: String?,
    bodyHtml: String? = null,
    fallbackBodyText: String? = null,
    bodyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bodyInteractionsEnabled: Boolean = true,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    referencePreviewState: NmbPostReferencePreviewState? = null,
    bodyMaxLines: Int = Int.MAX_VALUE,
    bodyOverflow: TextOverflow = TextOverflow.Clip,
    customBody: (@Composable () -> Unit)? = null,
    image: String?,
    ext: String?,
    onImageClick: (String, String?) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!title.isNullOrBlank() || !subtitle.isNullOrBlank()) {
            Column {
                title?.let { displayTitle ->
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                subtitle?.let { displaySubtitle ->
                    Text(
                        text = displaySubtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when {
            customBody != null -> customBody()
            !bodyHtml.isNullOrBlank() || !fallbackBodyText.isNullOrBlank() -> {
                if (referencePreviewState != null) {
                    NmbReferencedPostBody(
                        sourcePostId = sourcePostId ?: 0L,
                        html = bodyHtml,
                        fallbackText = fallbackBodyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                        referencePreviewState = referencePreviewState,
                        onLinkClick = onLinkClick,
                        maxLines = bodyMaxLines,
                        overflow = bodyOverflow
                    )
                } else if (shouldRenderNmbRichText(bodyHtml)) {
                    NmbRichTextText(
                        html = bodyHtml,
                        fallbackText = fallbackBodyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                        interactionsEnabled = bodyInteractionsEnabled,
                        onLinkClick = onLinkClick,
                        maxLines = bodyMaxLines,
                        overflow = bodyOverflow
                    )
                } else {
                    Text(
                        text = fallbackBodyText.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                        maxLines = bodyMaxLines,
                        overflow = bodyOverflow
                    )
                }
            }
        }

        if (!image.isNullOrBlank()) {
            ThreadImagePreview(
                image = image,
                ext = ext,
                onImageClick = onImageClick
            )
        }
    }
}

private fun Modifier.nmbPostInteractions(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?
): Modifier {
    if (onClick == null && onLongClick == null) {
        return this
    }
    return combinedClickable(
        onClick = onClick ?: {},
        onLongClick = onLongClick
    )
}

@Preview(name = "Catalog Card", widthDp = 412, heightDp = 720)
@Composable
private fun CatalogPostCardPreview() {
    FogIslandPreviewColumn {
        NmbPostCard(
            post = NmbPreviewSamples.forumThread,
            forumName = "欢乐恶搞",
            onClick = {},
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = 16.dp),
            bodyMaxLines = 6,
            bodyOverflow = TextOverflow.Ellipsis
        )
        NmbPostCard(
            post = NmbPreviewSamples.forumThreadWithoutTitle,
            forumName = "欢乐恶搞",
            onClick = {},
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = 16.dp),
            bodyMaxLines = 4,
            bodyOverflow = TextOverflow.Ellipsis
        )
        NmbPostCard(
            post = NmbPreviewSamples.forumThreadWithoutImage,
            forumName = "欢乐恶搞",
            onClick = {},
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = 16.dp),
            bodyMaxLines = 4,
            bodyOverflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Subscription Card", widthDp = 412, heightDp = 340)
@Composable
private fun SubscriptionPostCardPreview() {
    FogIslandPreviewColumn {
        NmbPostCard(
            post = NmbPreviewSamples.subscriptionThread,
            forumName = "综合版",
            onClick = {},
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = 16.dp),
            bodyMaxLines = 4,
            bodyOverflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "History Card", widthDp = 412, heightDp = 320)
@Composable
private fun HistoryPostCardPreview() {
    FogIslandPreviewColumn {
        NmbPostCard(
            post = NmbPreviewSamples.historyThread,
            forumName = "综合版",
            onClick = {},
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = 16.dp),
            bodyMaxLines = 4,
            bodyOverflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Search Card", widthDp = 412, heightDp = 520)
@Composable
private fun SearchPostCardPreview() {
    FogIslandPreviewColumn {
        NmbPostCard(
            hit = NmbPreviewSamples.searchThreadHit,
            query = "岛风",
            forumName = "技术版",
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        NmbPostCard(
            hit = NmbPreviewSamples.searchReplyHit,
            query = "岛风",
            forumName = "技术版",
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Preview(name = "Content Block", widthDp = 412, heightDp = 720)
@Composable
private fun NmbPostContentPreview() {
    FogIslandPreviewColumn {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            NmbPostContent(
                title = NmbPreviewSamples.forumThread.title,
                subtitle = NmbPreviewSamples.forumThread.name,
                bodyHtml = NmbPreviewSamples.forumThread.contentHtml,
                fallbackBodyText = NmbPreviewSamples.forumThread.contentText,
                bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                bodyInteractionsEnabled = true,
                bodyMaxLines = 6,
                bodyOverflow = TextOverflow.Ellipsis,
                image = NmbPreviewSamples.forumThread.image,
                ext = NmbPreviewSamples.forumThread.ext,
                onImageClick = { _, _ -> }
            )
            NmbPostContent(
                title = NmbPreviewSamples.directThreadShortcutUncached.title,
                subtitle = NmbPreviewSamples.directThreadShortcutUncached.name,
                image = null,
                ext = null,
                onImageClick = { _, _ -> }
            )
            NmbPostContent(
                title = NmbPreviewSamples.replyPost.title,
                subtitle = NmbPreviewSamples.replyPost.userHash,
                bodyHtml = NmbPreviewSamples.replyPost.contentHtml,
                fallbackBodyText = NmbPreviewSamples.replyPost.contentText,
                bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                bodyInteractionsEnabled = true,
                bodyMaxLines = 4,
                bodyOverflow = TextOverflow.Ellipsis,
                image = NmbPreviewSamples.replyPost.image,
                ext = NmbPreviewSamples.replyPost.ext,
                onImageClick = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Flat Item", widthDp = 412, heightDp = 640)
@Composable
private fun NmbPostFlatItemPreview() {
    FogIslandPreviewColumn(verticalSpacingDp = 0) {
        NmbPostFlatItem(
            post = NmbPreviewSamples.forumThread,
            onImageClick = { _, _ -> }
        )
        NmbPostFlatItem(
            post = NmbPreviewSamples.replyPost,
            onImageClick = { _, _ -> }
        )
    }
}
