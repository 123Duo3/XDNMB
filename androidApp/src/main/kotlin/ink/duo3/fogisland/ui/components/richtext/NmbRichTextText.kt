package ink.duo3.fogisland.ui.components.richtext

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.materialkolor.ktx.harmonize
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.NmbRichText
import ink.duo3.fogisland.shared.util.NmbRichTextSegment
import ink.duo3.fogisland.shared.util.parseNmbRichText
import ink.duo3.fogisland.ui.theme.LocalFogIslandDarkTheme
import ink.duo3.fogisland.ui.theme.LocalFogIslandUseMonet
import kotlinx.coroutines.launch

private const val NmbRichTextLinkAnnotationTag = "nmb_link"
private const val NmbRichTextHiddenAnnotationTag = "nmb_hidden"
private const val NmbHiddenRevealAnimationDurationMillis = 220

@Composable
fun NmbRichTextText(
    html: String?,
    fallbackText: String?,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    interactionsEnabled: Boolean = true,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    linkColor: Color? = null,
    referenceColor: Color? = null,
    internalLinkColorOverride: Color? = null,
    suppressInternalLinkUnderline: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val sourceHtml = html
        ?.takeIf { value -> value.isNotBlank() }
        ?: fallbackText.orEmpty()
    val richText = remember(sourceHtml) { parseNmbRichText(sourceHtml) }
    NmbRichTextText(
        richText = richText,
        style = style,
        color = color,
        modifier = modifier,
        interactionsEnabled = interactionsEnabled,
        onLinkClick = onLinkClick,
        linkColor = linkColor,
        referenceColor = referenceColor,
        internalLinkColorOverride = internalLinkColorOverride,
        suppressInternalLinkUnderline = suppressInternalLinkUnderline,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun NmbRichTextText(
    richText: NmbRichText,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    interactionsEnabled: Boolean = true,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    linkColor: Color? = null,
    referenceColor: Color? = null,
    internalLinkColorOverride: Color? = null,
    suppressInternalLinkUnderline: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val revealedHiddenGroupIds = remember(richText, interactionsEnabled) {
        mutableStateListOf<Int>()
    }
    val hiddenRevealProgress = remember(richText, interactionsEnabled) {
        mutableStateMapOf<Int, Float>()
    }
    val revealedHiddenGroupSnapshot = if (interactionsEnabled) {
        revealedHiddenGroupIds.toSet()
    } else {
        emptySet()
    }
    val hiddenRevealProgressSnapshot = if (interactionsEnabled) {
        hiddenRevealProgress.toMap()
    } else {
        emptyMap()
    }
    var textLayoutResult by remember(richText) { mutableStateOf<TextLayoutResult?>(null) }
    val currentLayoutResult by rememberUpdatedState(textLayoutResult)
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val coroutineScope = rememberCoroutineScope()
    val resolvedLinkColor = linkColor ?: MaterialTheme.colorScheme.tertiary
    val resolvedReferenceColor = referenceColor ?: MaterialTheme.colorScheme.tertiary
    val hiddenBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val hiddenTextColor = hiddenBackgroundColor.copy(alpha = 0.12f)
    val harmonizeTargetColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = LocalFogIslandDarkTheme.current
    val useMonet = LocalFogIslandUseMonet.current
    val labelMediumStyle = MaterialTheme.typography.labelMedium
    val replyColor = resolvedReferenceColor
    val harmonizedPureGreen = remember(harmonizeTargetColor, isDarkTheme, useMonet) {
        val baseColor = if (isDarkTheme) {
            Color(0xFF81C784)
        } else {
            Color(0xFF4CAF50)
        }
        if (useMonet) baseColor.harmonize(harmonizeTargetColor) else baseColor
    }
    val harmonizedPureBlue = remember(harmonizeTargetColor, isDarkTheme, useMonet) {
        val baseColor = if (isDarkTheme) {
            Color(0xFF64B5F6)
        } else {
            Color(0xFF2196F3)
        }
        if (useMonet) baseColor.harmonize(harmonizeTargetColor) else baseColor
    }
    val deepSkyBlueColor = remember(harmonizeTargetColor, isDarkTheme, useMonet) {
        val baseColor = if (isDarkTheme) {
            Color(0xFF4FC3F7)
        } else {
            Color(0xFF03A9F4)
        }
        if (useMonet) baseColor.harmonize(harmonizeTargetColor) else baseColor
    }
    val harmonizedPureRed = remember(harmonizeTargetColor, isDarkTheme, useMonet) {
        val baseColor = if (isDarkTheme) {
            Color(0xFFE57373)
        } else {
            Color(0xFFF44336)
        }
        if (useMonet) baseColor.harmonize(harmonizeTargetColor) else baseColor
    }
    val resolvedBaseFontSize = when {
        style.fontSize != TextUnit.Unspecified -> style.fontSize
        else -> MaterialTheme.typography.bodyMedium.fontSize
    }
    val inlinePreviewParts = remember(richText) {
        buildInlinePreviewParts(richText)
    }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val annotatedText = remember(
        richText,
        inlinePreviewParts,
        color,
        resolvedLinkColor,
        resolvedReferenceColor,
        internalLinkColorOverride,
        hiddenTextColor,
        labelMediumStyle,
        resolvedBaseFontSize,
        revealedHiddenGroupSnapshot,
        hiddenRevealProgressSnapshot,
        replyColor,
        harmonizedPureGreen,
        harmonizedPureBlue,
        deepSkyBlueColor,
        harmonizedPureRed,
        harmonizeTargetColor,
        useMonet,
        suppressInternalLinkUnderline
    ) {
        val builder = AnnotatedString.Builder()
        inlinePreviewParts.forEach { part ->
            when (part) {
                is InlinePreviewPart.TextPart -> {
                    builder.append(
                        part.richText.toAnnotatedString(
                            normalColor = color,
                            linkColor = resolvedLinkColor,
                            referenceColor = resolvedReferenceColor,
                            internalLinkColorOverride = internalLinkColorOverride,
                            hiddenTextColor = hiddenTextColor,
                            labelMediumStyle = labelMediumStyle,
                            baseFontSize = resolvedBaseFontSize,
                            revealedHiddenGroupIds = revealedHiddenGroupSnapshot,
                            hiddenRevealProgress = hiddenRevealProgressSnapshot,
                            replyColor = replyColor,
                            harmonizedPureGreen = harmonizedPureGreen,
                            harmonizedPureBlue = harmonizedPureBlue,
                            deepSkyBlueColor = deepSkyBlueColor,
                            harmonizedPureRed = harmonizedPureRed,
                            harmonizeTargetColor = harmonizeTargetColor,
                            useMonet = useMonet,
                            suppressInternalLinkUnderline = suppressInternalLinkUnderline
                        )
                    )
                }

                is InlinePreviewPart.PreviewPart -> {
                    builder.appendInlineContent(
                        id = part.inlineContentId,
                        alternateText = part.richText.plainText
                    )
                }
            }
        }
        builder.toAnnotatedString()
    }
    val inlineContent = remember(
        inlinePreviewParts,
        density,
        textMeasurer,
        style,
        labelMediumStyle,
        suppressInternalLinkUnderline,
        currentOnLinkClick
    ) {
        inlinePreviewParts
            .filterIsInstance<InlinePreviewPart.PreviewPart>()
            .associate { part ->
                part.inlineContentId to InlineTextContent(
                    placeholder = Placeholder(
                        width = estimateInlinePreviewChipWidth(
                            text = part.richText.plainText,
                            textStyle = labelMediumStyle,
                            textMeasurer = textMeasurer,
                            density = density
                        ),
                        height = estimateInlinePreviewChipHeight(
                            surroundingStyle = style,
                            density = density
                        ),
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InlinePreviewChip(
                            richText = part.richText.stripInlinePreviewStyling(),
                            clickTarget = part.clickTarget,
                            style = labelMediumStyle,
                            onLinkClick = currentOnLinkClick,
                            suppressInternalLinkUnderline = suppressInternalLinkUnderline
                        )
                    }
                }
            }
    }

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        modifier = modifier
            .drawBehind {
                val layout = currentLayoutResult ?: return@drawBehind
                drawHiddenTextBackgrounds(
                    text = annotatedText,
                    layout = layout,
                    backgroundColor = hiddenBackgroundColor,
                    cornerRadius = 4.dp.toPx(),
                    horizontalInset = 1.dp.toPx(),
                    verticalInset = 1.dp.toPx(),
                    hiddenRevealProgress = hiddenRevealProgressSnapshot
                )
            }
            .pointerInput(annotatedText, interactionsEnabled) {
                if (!interactionsEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    val layout = currentLayoutResult ?: return@awaitEachGesture
                    val characterOffset = layout.getOffsetForPosition(up.position)

                    val hiddenGroupId = annotatedText.getStringAnnotations(
                        tag = NmbRichTextHiddenAnnotationTag,
                        start = characterOffset,
                        end = characterOffset
                    ).firstOrNull()
                        ?.item
                        ?.toIntOrNull()
                        ?.takeIf { groupId ->
                            groupId !in revealedHiddenGroupIds && groupId !in hiddenRevealProgress
                        }

                    if (hiddenGroupId != null) {
                        down.consume()
                        up.consume()
                        hiddenRevealProgress[hiddenGroupId] = 0f
                        coroutineScope.launch {
                            val animatable = Animatable(0f)
                            animatable.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = NmbHiddenRevealAnimationDurationMillis,
                                    easing = FastOutSlowInEasing
                                )
                            ) {
                                hiddenRevealProgress[hiddenGroupId] = value
                            }
                            hiddenRevealProgress.remove(hiddenGroupId)
                            revealedHiddenGroupIds.add(hiddenGroupId)
                        }
                        return@awaitEachGesture
                    }

                    val url = annotatedText.getStringAnnotations(
                        tag = NmbRichTextLinkAnnotationTag,
                        start = characterOffset,
                        end = characterOffset
                    ).firstOrNull()?.item?.let(::decodeNmbLinkTarget)

                    if (url != null && currentOnLinkClick != null) {
                        down.consume()
                        up.consume()
                        currentOnLinkClick?.invoke(url)
                    }
                }
            },
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
        }
    )
}

private fun NmbRichText.toAnnotatedString(
    normalColor: Color,
    linkColor: Color,
    referenceColor: Color,
    internalLinkColorOverride: Color?,
    hiddenTextColor: Color,
    labelMediumStyle: TextStyle,
    baseFontSize: TextUnit,
    revealedHiddenGroupIds: Set<Int>,
    hiddenRevealProgress: Map<Int, Float>,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    deepSkyBlueColor: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean,
    suppressInternalLinkUnderline: Boolean
): AnnotatedString {
    val builder = AnnotatedString.Builder()

    segments.forEachIndexed { index, segment ->
        val start = builder.length
        builder.append(segment.text)
        val end = builder.length
        if (start >= end) {
            return@forEachIndexed
        }

        val segmentColor = segment.resolveDisplayColor(
            normalColor = normalColor,
            linkColor = linkColor,
            referenceColor = referenceColor,
            internalLinkColorOverride = internalLinkColorOverride,
            hiddenTextColor = hiddenTextColor,
            revealedHidden = segment.hiddenGroupId?.let { it in revealedHiddenGroupIds } ?: false,
            revealProgress = segment.hiddenGroupId?.let { hiddenRevealProgress[it] } ?: 0f,
            replyColor = replyColor,
            harmonizedPureGreen = harmonizedPureGreen,
            harmonizedPureBlue = harmonizedPureBlue,
            deepSkyBlueColor = deepSkyBlueColor,
            harmonizedPureRed = harmonizedPureRed,
            harmonizeTargetColor = harmonizeTargetColor,
            useMonet = useMonet
        )

        val shouldUnderlineLink = when (segment.linkTarget) {
            is NmbLinkTarget.ExternalUrl -> true
            null -> false
            else -> !suppressInternalLinkUnderline && !segment.suppressLinkUnderline
        }
        val textDecoration = when {
            shouldUnderlineLink && segment.isStrikethrough ->
                TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                )

            shouldUnderlineLink || segment.isUnderline -> TextDecoration.Underline
            segment.isStrikethrough -> TextDecoration.LineThrough
            else -> null
        }

        val hasExplicitStyle = segmentColor != Color.Unspecified ||
            segment.isBold ||
            segment.isItalic ||
            segment.isSmall ||
            segment.isCode ||
            segment.useLabelMediumStyle ||
            textDecoration != null

        if (hasExplicitStyle) {
            builder.addStyle(
                style = SpanStyle(
                    color = segmentColor,
                    fontWeight = when {
                        segment.isBold -> FontWeight.Bold
                        segment.useLabelMediumStyle -> labelMediumStyle.fontWeight
                        else -> null
                    },
                    fontStyle = if (segment.isItalic) FontStyle.Italic else null,
                    fontSize = when {
                        segment.useLabelMediumStyle && labelMediumStyle.fontSize != TextUnit.Unspecified ->
                            labelMediumStyle.fontSize
                        segment.isSmall -> (baseFontSize.value * 0.85f).sp
                        else -> TextUnit.Unspecified
                    },
                    textDecoration = textDecoration,
                    fontFamily = when {
                        segment.isCode -> FontFamily.Monospace
                        segment.useLabelMediumStyle -> labelMediumStyle.fontFamily
                        else -> null
                    }
                ),
                start = start,
                end = end
            )
        }

        val linkTarget = segment.linkTarget
        if (linkTarget != null) {
            builder.addStringAnnotation(
                tag = NmbRichTextLinkAnnotationTag,
                annotation = encodeNmbLinkTarget(linkTarget),
                start = start,
                end = end
            )
        }

        val hiddenGroupId = segment.hiddenGroupId
        if (segment.isHidden && hiddenGroupId != null && hiddenGroupId !in revealedHiddenGroupIds) {
            builder.addStringAnnotation(
                tag = NmbRichTextHiddenAnnotationTag,
                annotation = hiddenGroupId.toString(),
                start = start,
                end = end
            )
        }

    }

    return builder.toAnnotatedString()
}

private fun NmbRichTextSegment.resolveDisplayColor(
    normalColor: Color,
    linkColor: Color,
    referenceColor: Color,
    internalLinkColorOverride: Color?,
    hiddenTextColor: Color,
    revealedHidden: Boolean,
    revealProgress: Float,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    deepSkyBlueColor: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean
): Color {
    val segmentColor = color
    val visibleColor = when {
        linkTarget is NmbLinkTarget.ExternalUrl -> linkColor
        linkTarget != null && useInternalLinkColorOverride && internalLinkColorOverride != null ->
            internalLinkColorOverride
        linkTarget != null -> referenceColor
        segmentColor != null -> parseNmbHtmlColor(segmentColor)?.let { parsedColor ->
            remapNmbHtmlColor(
                rawColor = parsedColor,
                replyColor = replyColor,
                harmonizedPureGreen = harmonizedPureGreen,
                harmonizedPureBlue = harmonizedPureBlue,
                deepSkyBlueColor = deepSkyBlueColor,
                harmonizedPureRed = harmonizedPureRed,
                harmonizeTargetColor = harmonizeTargetColor,
                useMonet = useMonet
            )
        } ?: normalColor
        else -> normalColor
    }

    if (!isHidden || revealedHidden) {
        return visibleColor
    }

    val targetColor = if (visibleColor != Color.Unspecified) visibleColor else normalColor
    return lerp(hiddenTextColor, targetColor, revealProgress)
}

private fun encodeNmbLinkTarget(target: NmbLinkTarget): String {
    return when (target) {
        is NmbLinkTarget.PostReference -> "post:${target.postId}"
        is NmbLinkTarget.Thread ->
            "thread:${target.threadId}:${target.targetPostId.orEmptyAnnotationPart()}:${target.targetPage.orEmptyAnnotationPart()}"
        is NmbLinkTarget.ExternalUrl -> "url:${target.url}"
    }
}

private fun decodeNmbLinkTarget(annotation: String): NmbLinkTarget? {
    val type = annotation.substringBefore(':')
    val payload = annotation.substringAfter(':', missingDelimiterValue = "")
    if (payload.isBlank()) {
        return null
    }

    return when (type) {
        "post" -> payload.toLongOrNull()?.let(NmbLinkTarget::PostReference)
        "thread" -> {
            val parts = payload.split(':', limit = 3)
            val threadId = parts.getOrNull(0)?.toLongOrNull() ?: return null
            NmbLinkTarget.Thread(
                threadId = threadId,
                targetPostId = parts.getOrNull(1)?.nullIfBlank()?.toLongOrNull(),
                targetPage = parts.getOrNull(2)?.nullIfBlank()?.toIntOrNull()
            )
        }
        "url" -> NmbLinkTarget.ExternalUrl(payload)
        else -> null
    }
}

private fun Any?.orEmptyAnnotationPart(): String = this?.toString().orEmpty()

private fun String.nullIfBlank(): String? = takeIf { it.isNotBlank() }

private fun parseNmbHtmlColor(rawColor: String): Color? {
    return runCatching { Color(AndroidColor.parseColor(rawColor)) }.getOrNull()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHiddenTextBackgrounds(
    text: AnnotatedString,
    layout: TextLayoutResult,
    backgroundColor: Color,
    cornerRadius: Float,
    horizontalInset: Float,
    verticalInset: Float,
    hiddenRevealProgress: Map<Int, Float>
) {
    val hiddenAnnotations = text.getStringAnnotations(
        tag = NmbRichTextHiddenAnnotationTag,
        start = 0,
        end = text.length
    ).sortedBy { it.start }
        .mergeAdjacentHiddenAnnotations()

    hiddenAnnotations.forEachIndexed { index, annotation ->
        val revealProgress = annotation.id.toIntOrNull()?.let { hiddenRevealProgress[it] } ?: 0f
        val firstLine = layout.getLineForOffset(annotation.start)
        val lastCharacterOffset = (annotation.end - 1).coerceAtLeast(annotation.start)
        val lastLine = layout.getLineForOffset(lastCharacterOffset)

        for (lineIndex in firstLine..lastLine) {
            val lineStart = maxOf(annotation.start, layout.getLineStart(lineIndex))
            val lineEndExclusive = minOf(
                annotation.end,
                layout.getLineEnd(lineIndex, visibleEnd = true)
            )
            if (lineStart >= lineEndExclusive) {
                continue
            }

            val firstBox = layout.getBoundingBox(lineStart)
            val lastBox = layout.getBoundingBox(lineEndExclusive - 1)
            val lineVisibleStart = layout.getLineStart(lineIndex)
            val lineVisibleEnd = layout.getLineEnd(lineIndex, visibleEnd = true)
            val previousChar = text.text.getOrNull(lineStart - 1)
            val nextChar = text.text.getOrNull(lineEndExclusive)

            val hasPreviousAdjacentHidden = hiddenAnnotations.getOrNull(index - 1)?.let { previous ->
                previous.end == lineStart &&
                    lineStart > lineVisibleStart &&
                    previous.end > previous.start &&
                    layout.getLineForOffset(previous.end - 1) == lineIndex
            } == true

            val hasNextAdjacentHidden = hiddenAnnotations.getOrNull(index + 1)?.let { next ->
                next.start == lineEndExclusive &&
                    lineEndExclusive < lineVisibleEnd &&
                    next.end > next.start &&
                    layout.getLineForOffset(next.start) == lineIndex
            } == true

            val touchesPreviousText = !hasPreviousAdjacentHidden &&
                lineStart > lineVisibleStart &&
                previousChar != null &&
                !previousChar.isWhitespace()

            val touchesNextText = !hasNextAdjacentHidden &&
                lineEndExclusive < lineVisibleEnd &&
                nextChar != null &&
                !nextChar.isWhitespace()

            val adjustedHorizontalInset = minOf(horizontalInset, firstBox.width / 2f, lastBox.width / 2f)
            val adjustedVerticalInset = minOf(verticalInset, firstBox.height / 2f)
            val rect = Rect(
                left = firstBox.left + if (hasPreviousAdjacentHidden || touchesPreviousText) adjustedHorizontalInset else 0f,
                top = firstBox.top + adjustedVerticalInset,
                right = lastBox.right - if (hasNextAdjacentHidden || touchesNextText) adjustedHorizontalInset else 0f,
                bottom = firstBox.bottom - adjustedVerticalInset
            )

            drawRoundRect(
                color = backgroundColor.copy(alpha = backgroundColor.alpha * (1f - revealProgress)),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

private data class GroupedAnnotationRange(
    val id: String,
    val start: Int,
    val end: Int
)

private fun List<AnnotatedString.Range<String>>.mergeAdjacentGroupedAnnotations(): List<GroupedAnnotationRange> {
    if (isEmpty()) {
        return emptyList()
    }

    val merged = mutableListOf<GroupedAnnotationRange>()
    forEach { annotation ->
        val previous = merged.lastOrNull()
        if (previous != null &&
            previous.id == annotation.item &&
            previous.end == annotation.start
        ) {
            merged[merged.lastIndex] = previous.copy(end = annotation.end)
        } else {
            merged += GroupedAnnotationRange(
                id = annotation.item,
                start = annotation.start,
                end = annotation.end
            )
        }
    }
    return merged
}

private fun List<AnnotatedString.Range<String>>.mergeAdjacentHiddenAnnotations(): List<GroupedAnnotationRange> {
    return mergeAdjacentGroupedAnnotations()
}

private fun remapNmbHtmlColor(
    rawColor: Color,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    deepSkyBlueColor: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean
): Color {
    return when (rawColor.toArgb()) {
        "#789922".toColorInt() -> replyColor
        "#00FF00".toColorInt() -> harmonizedPureGreen
        "#00BFFF".toColorInt() -> deepSkyBlueColor
        "#FF0000".toColorInt() -> harmonizedPureRed
        "#0000FF".toColorInt() -> harmonizedPureBlue
        else -> if (useMonet) rawColor.harmonize(harmonizeTargetColor) else rawColor
    }
}
