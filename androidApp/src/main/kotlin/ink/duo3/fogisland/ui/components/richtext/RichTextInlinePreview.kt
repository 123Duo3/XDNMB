package ink.duo3.fogisland.ui.components.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.NmbRichText
import ink.duo3.fogisland.shared.util.NmbRichTextSegment

private val InlinePreviewChipHorizontalPadding = 2.dp
private val InlinePreviewChipVerticalPadding = 1.dp

internal sealed interface InlinePreviewPart {
    data class TextPart(val richText: NmbRichText) : InlinePreviewPart
    data class PreviewPart(
        val inlineContentId: String,
        val richText: NmbRichText,
        val clickTarget: NmbLinkTarget?
    ) : InlinePreviewPart
}

@Composable
internal fun InlinePreviewChip(
    richText: NmbRichText,
    clickTarget: NmbLinkTarget?,
    style: TextStyle,
    onLinkClick: ((NmbLinkTarget) -> Unit)?,
    suppressInternalLinkUnderline: Boolean
) {
    val shape = RoundedCornerShape(4.dp)
    Surface(
        modifier = if (clickTarget != null && onLinkClick != null) {
            Modifier
                .clip(shape)
                .clickable { onLinkClick(clickTarget) }
        } else {
            Modifier
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = InlinePreviewChipHorizontalPadding,
                vertical = InlinePreviewChipVerticalPadding
            )
        ) {
            NmbRichTextText(
                richText = richText,
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                interactionsEnabled = false,
                linkColor = MaterialTheme.colorScheme.onSurface,
                suppressInternalLinkUnderline = suppressInternalLinkUnderline,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

internal fun buildInlinePreviewParts(richText: NmbRichText): List<InlinePreviewPart> {
    if (richText.segments.none { it.inlinePreviewGroupId != null }) {
        return listOf(InlinePreviewPart.TextPart(richText))
    }

    val parts = mutableListOf<InlinePreviewPart>()
    val textSegments = mutableListOf<NmbRichTextSegment>()
    val previewSegments = mutableListOf<NmbRichTextSegment>()
    var currentPreviewGroupId: Int? = null

    fun flushText() {
        if (textSegments.isEmpty()) return
        parts += InlinePreviewPart.TextPart(NmbRichText(textSegments.toList()))
        textSegments.clear()
    }

    fun flushPreview() {
        val previewGroupId = currentPreviewGroupId ?: return
        if (previewSegments.isEmpty()) return
        parts += InlinePreviewPart.PreviewPart(
            inlineContentId = "nmb_inline_preview_$previewGroupId",
            richText = NmbRichText(previewSegments.toList()),
            clickTarget = previewSegments.firstNotNullOfOrNull { segment -> segment.linkTarget }
        )
        previewSegments.clear()
        currentPreviewGroupId = null
    }

    richText.segments.forEach { segment ->
        val previewGroupId = segment.inlinePreviewGroupId
        if (previewGroupId == null) {
            flushPreview()
            textSegments += segment
        } else {
            flushText()
            if (currentPreviewGroupId != previewGroupId) {
                flushPreview()
                currentPreviewGroupId = previewGroupId
            }
            previewSegments += segment
        }
    }

    flushPreview()
    flushText()
    return parts
}

internal fun NmbRichText.stripInlinePreviewStyling(): NmbRichText {
    return NmbRichText(
        segments = segments.map { segment ->
            segment.copy(
                inlinePreviewGroupId = null,
                useLabelMediumStyle = false
            )
        }
    )
}

internal fun estimateInlinePreviewChipWidth(
    text: String,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density
): TextUnit {
    val measuredWidthPx = textMeasurer.measure(
        text = AnnotatedString(text),
        style = textStyle,
        maxLines = 1
    ).size.width.toFloat()
    val horizontalPaddingPx = with(density) { (InlinePreviewChipHorizontalPadding * 2).toPx() }
    return with(density) { (measuredWidthPx + horizontalPaddingPx).toSp() }
}

internal fun estimateInlinePreviewChipHeight(
    surroundingStyle: TextStyle,
    density: Density
): TextUnit {
    val surroundingHeightPx = with(density) {
        val lineHeight = surroundingStyle.lineHeight.takeIf { it != TextUnit.Unspecified }
            ?: ((surroundingStyle.fontSize.takeIf { it != TextUnit.Unspecified } ?: 15.sp) * 1.15f)
        lineHeight.toPx()
    }
    return with(density) { surroundingHeightPx.toSp() }
}
