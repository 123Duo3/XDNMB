package ink.duo3.fogisland.ui.components

import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.materialkolor.ktx.harmonize
import ink.duo3.fogisland.shared.util.NmbRichText
import ink.duo3.fogisland.shared.util.NmbRichTextSegment
import ink.duo3.fogisland.shared.util.parseNmbRichText
import ink.duo3.fogisland.ui.theme.LocalFogIslandDarkTheme
import ink.duo3.fogisland.ui.theme.LocalFogIslandUseMonet
import kotlinx.coroutines.launch

private const val NmbRichTextUrlAnnotationTag = "nmb_url"
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
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    val revealedHiddenSegmentIndices = remember(richText, interactionsEnabled) {
        mutableStateListOf<Int>()
    }
    val hiddenRevealProgress = remember(richText, interactionsEnabled) {
        mutableStateMapOf<Int, Float>()
    }
    val revealedHiddenSegmentSnapshot = remember(
        interactionsEnabled,
        revealedHiddenSegmentIndices.size
    ) {
        if (interactionsEnabled) {
            revealedHiddenSegmentIndices.toSet()
        } else {
            emptySet()
        }
    }
    val hiddenRevealProgressSnapshot = remember(interactionsEnabled, hiddenRevealProgress.size) {
        if (interactionsEnabled) {
            hiddenRevealProgress.toMap()
        } else {
            emptyMap()
        }
    }
    var textLayoutResult by remember(richText) { mutableStateOf<TextLayoutResult?>(null) }
    val currentLayoutResult by rememberUpdatedState(textLayoutResult)
    val coroutineScope = rememberCoroutineScope()
    val linkColor = MaterialTheme.colorScheme.primary
    val hiddenBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val hiddenTextColor = hiddenBackgroundColor.copy(alpha = 0.12f)
    val harmonizeTargetColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = LocalFogIslandDarkTheme.current
    val useMonet = LocalFogIslandUseMonet.current
    val replyColor = remember(harmonizeTargetColor, isDarkTheme, useMonet) {
        val baseColor = if (isDarkTheme) {
            Color(0xFFDCE775)
        } else {
            Color(0xFFAFB42B)
        }
        if (useMonet) baseColor.harmonize(harmonizeTargetColor) else baseColor
    }
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
    val annotatedText = remember(
        richText,
        color,
        linkColor,
        hiddenTextColor,
        resolvedBaseFontSize,
        revealedHiddenSegmentSnapshot,
        hiddenRevealProgressSnapshot,
        replyColor,
        harmonizedPureGreen,
        harmonizedPureBlue,
        harmonizedPureRed,
        harmonizeTargetColor,
        useMonet
    ) {
        richText.toAnnotatedString(
            normalColor = color,
            linkColor = linkColor,
            hiddenTextColor = hiddenTextColor,
            baseFontSize = resolvedBaseFontSize,
            revealedHiddenSegmentIndices = revealedHiddenSegmentSnapshot,
            hiddenRevealProgress = hiddenRevealProgressSnapshot,
            replyColor = replyColor,
            harmonizedPureGreen = harmonizedPureGreen,
            harmonizedPureBlue = harmonizedPureBlue,
            harmonizedPureRed = harmonizedPureRed,
            harmonizeTargetColor = harmonizeTargetColor,
            useMonet = useMonet
        )
    }

    Text(
        text = annotatedText,
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

                    val hiddenIndex = annotatedText.getStringAnnotations(
                        tag = NmbRichTextHiddenAnnotationTag,
                        start = characterOffset,
                        end = characterOffset
                    ).firstOrNull()
                        ?.item
                        ?.toIntOrNull()
                        ?.takeIf { index ->
                            index !in revealedHiddenSegmentIndices && index !in hiddenRevealProgress
                        }

                    if (hiddenIndex != null) {
                        down.consume()
                        up.consume()
                        hiddenRevealProgress[hiddenIndex] = 0f
                        coroutineScope.launch {
                            val animatable = Animatable(0f)
                            animatable.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = NmbHiddenRevealAnimationDurationMillis,
                                    easing = FastOutSlowInEasing
                                )
                            ) {
                                hiddenRevealProgress[hiddenIndex] = value
                            }
                            hiddenRevealProgress.remove(hiddenIndex)
                            revealedHiddenSegmentIndices.add(hiddenIndex)
                        }
                        return@awaitEachGesture
                    }

                    val url = annotatedText.getStringAnnotations(
                        tag = NmbRichTextUrlAnnotationTag,
                        start = characterOffset,
                        end = characterOffset
                    ).firstOrNull()?.item

                    if (url != null) {
                        down.consume()
                        up.consume()
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
    hiddenTextColor: Color,
    baseFontSize: TextUnit,
    revealedHiddenSegmentIndices: Set<Int>,
    hiddenRevealProgress: Map<Int, Float>,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean
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
            hiddenTextColor = hiddenTextColor,
            revealedHidden = index in revealedHiddenSegmentIndices,
            revealProgress = hiddenRevealProgress[index] ?: 0f,
            replyColor = replyColor,
            harmonizedPureGreen = harmonizedPureGreen,
            harmonizedPureBlue = harmonizedPureBlue,
            harmonizedPureRed = harmonizedPureRed,
            harmonizeTargetColor = harmonizeTargetColor,
            useMonet = useMonet
        )

        val textDecoration = when {
            segment.href != null && segment.isStrikethrough ->
                TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                )

            segment.href != null || segment.isUnderline -> TextDecoration.Underline
            segment.isStrikethrough -> TextDecoration.LineThrough
            else -> null
        }

        val hasExplicitStyle = segmentColor != Color.Unspecified ||
            segment.isBold ||
            segment.isItalic ||
            segment.isSmall ||
            segment.isCode ||
            textDecoration != null

        if (hasExplicitStyle) {
            builder.addStyle(
                style = SpanStyle(
                    color = segmentColor,
                    fontWeight = if (segment.isBold) FontWeight.Bold else null,
                    fontStyle = if (segment.isItalic) FontStyle.Italic else null,
                    fontSize = if (segment.isSmall) (baseFontSize.value * 0.85f).sp else TextUnit.Unspecified,
                    textDecoration = textDecoration,
                    fontFamily = if (segment.isCode) FontFamily.Monospace else null
                ),
                start = start,
                end = end
            )
        }

        val href = segment.href
        if (href != null) {
            builder.addStringAnnotation(
                tag = NmbRichTextUrlAnnotationTag,
                annotation = href,
                start = start,
                end = end
            )
        }

        if (segment.isHidden && index !in revealedHiddenSegmentIndices) {
            builder.addStringAnnotation(
                tag = NmbRichTextHiddenAnnotationTag,
                annotation = index.toString(),
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
    hiddenTextColor: Color,
    revealedHidden: Boolean,
    revealProgress: Float,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean
): Color {
    val href = href
    val segmentColor = color
    val visibleColor = when {
        href != null -> linkColor
        segmentColor != null -> parseNmbHtmlColor(segmentColor)?.let { parsedColor ->
            remapNmbHtmlColor(
                rawColor = parsedColor,
                replyColor = replyColor,
                harmonizedPureGreen = harmonizedPureGreen,
                harmonizedPureBlue = harmonizedPureBlue,
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

    hiddenAnnotations.forEachIndexed { index, annotation ->
        val revealProgress = annotation.item.toIntOrNull()?.let { hiddenRevealProgress[it] } ?: 0f
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

private fun remapNmbHtmlColor(
    rawColor: Color,
    replyColor: Color,
    harmonizedPureGreen: Color,
    harmonizedPureBlue: Color,
    harmonizedPureRed: Color,
    harmonizeTargetColor: Color,
    useMonet: Boolean
): Color {
    return when (rawColor.toArgb()) {
        "#789922".toColorInt() -> replyColor
        "#00FF00".toColorInt() -> harmonizedPureGreen
        "#FF0000".toColorInt() -> harmonizedPureRed
        "#0000FF".toColorInt() -> harmonizedPureBlue
        else -> if (useMonet) rawColor.harmonize(harmonizeTargetColor) else rawColor
    }
}
