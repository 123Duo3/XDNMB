package ink.duo3.fogisland.ui.components.imageviewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

internal const val IMAGE_VIEWER_MIN_SCALE = 1f
internal const val IMAGE_VIEWER_MAX_SCALE = 5f
internal const val IMAGE_VIEWER_TEMP_MIN_SCALE = 0.85f
internal const val IMAGE_VIEWER_TEMP_MAX_SCALE = 5.5f
internal const val IMAGE_VIEWER_DOUBLE_TAP_SCALE_MULTIPLIER = 2f

private const val LONG_IMAGE_RATIO_THRESHOLD = 3f

internal data class ImageViewerLayout(
    val viewportSize: IntSize,
    val baseSize: Size,
    val baseOrigin: Offset,
    val resetScale: Float,
    val isLongImage: Boolean
)

internal data class ImageViewerTranslationBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

internal fun calculateImageViewerLayout(
    viewportSize: IntSize,
    imageSize: IntSize?,
    maxLongImageWidthPx: Float
): ImageViewerLayout? {
    val intrinsicSize = imageSize ?: return null
    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()
    val imageWidth = intrinsicSize.width.toFloat()
    val imageHeight = intrinsicSize.height.toFloat()
    if (viewportWidth <= 0f || viewportHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return null
    }

    val isLongImage = (imageHeight / imageWidth) > LONG_IMAGE_RATIO_THRESHOLD
    val resetScale = if (isLongImage) {
        min(viewportWidth, maxLongImageWidthPx) / imageWidth
    } else {
        min(viewportWidth / imageWidth, viewportHeight / imageHeight)
    }
    val scaledWidth = imageWidth * resetScale
    val scaledHeight = imageHeight * resetScale
    val baseSize = Size(
        width = imageWidth,
        height = imageHeight
    )

    val baseOrigin = Offset(
        x = (viewportWidth - scaledWidth) / 2f,
        y = if (isLongImage) 0f else (viewportHeight - scaledHeight) / 2f
    )

    return ImageViewerLayout(
        viewportSize = viewportSize,
        baseSize = baseSize,
        baseOrigin = baseOrigin,
        resetScale = resetScale,
        isLongImage = isLongImage
    )
}

internal fun calculateImageViewerMinimumScale(
    layout: ImageViewerLayout
): Float {
    return min(
        layout.viewportSize.width.toFloat() / layout.baseSize.width,
        layout.viewportSize.height.toFloat() / layout.baseSize.height
    )
}

internal fun coerceImageViewerGestureScale(
    scale: Float,
    minimumScale: Float
): Float {
    return scale.coerceIn(
        minimumScale * IMAGE_VIEWER_TEMP_MIN_SCALE,
        IMAGE_VIEWER_TEMP_MAX_SCALE
    )
}

internal fun coerceImageViewerSettledScale(
    scale: Float,
    minimumScale: Float
): Float {
    return scale.coerceIn(minimumScale, IMAGE_VIEWER_MAX_SCALE)
}

internal fun calculateImageViewerScaledTranslation(
    currentTranslation: Offset,
    currentScale: Float,
    targetScale: Float,
    anchor: Offset,
    destinationAnchor: Offset
): Offset {
    if (currentScale <= 0f) {
        return currentTranslation
    }

    val contentAnchor = Offset(
        x = (anchor.x - currentTranslation.x) / currentScale,
        y = (anchor.y - currentTranslation.y) / currentScale
    )

    return Offset(
        x = destinationAnchor.x - (contentAnchor.x * targetScale),
        y = destinationAnchor.y - (contentAnchor.y * targetScale)
    )
}

internal fun clampImageViewerTranslation(
    translation: Offset,
    layout: ImageViewerLayout,
    scale: Float
): Offset {
    val bounds = calculateImageViewerTranslationBounds(
        layout = layout,
        scale = scale
    )

    return Offset(
        x = translation.x.coerceIn(bounds.minX, bounds.maxX),
        y = translation.y.coerceIn(bounds.minY, bounds.maxY)
    )
}

internal fun calculateImageViewerResetTranslation(layout: ImageViewerLayout): Offset {
    return layout.baseOrigin
}

internal fun calculateImageViewerTranslationBounds(
    layout: ImageViewerLayout,
    scale: Float
): ImageViewerTranslationBounds {
    val viewportWidth = layout.viewportSize.width.toFloat()
    val viewportHeight = layout.viewportSize.height.toFloat()
    val scaledWidth = layout.baseSize.width * scale
    val scaledHeight = layout.baseSize.height * scale

    val centeredX = (viewportWidth - scaledWidth) / 2f
    val centeredY = (viewportHeight - scaledHeight) / 2f

    return ImageViewerTranslationBounds(
        minX = if (scaledWidth <= viewportWidth) centeredX else viewportWidth - scaledWidth,
        maxX = if (scaledWidth <= viewportWidth) centeredX else 0f,
        minY = if (scaledHeight <= viewportHeight) centeredY else viewportHeight - scaledHeight,
        maxY = if (scaledHeight <= viewportHeight) centeredY else 0f
    )
}
