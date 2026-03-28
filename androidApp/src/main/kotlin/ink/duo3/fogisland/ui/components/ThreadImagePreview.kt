package ink.duo3.fogisland.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.util.buildNmbThumbImageUrl
import ink.duo3.fogisland.utils.resolveNmbImageFallbackUrl
import kotlinx.coroutines.launch
import kotlin.math.min

private const val THREAD_IMAGE_PREVIEW_HEIGHT_DP = 128
private const val THREAD_IMAGE_PREVIEW_MIN_WIDTH_DP = 64
private const val THREAD_IMAGE_PREVIEW_CORNER_RADIUS_DP = 8

private data class ThreadImagePreviewLayout(
    val widthPx: Float,
    val alignment: Alignment
)

@Composable
fun ThreadImagePreview(
    image: String,
    ext: String?,
    onImageClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        RepositoryProvider.provideForumRepository(context.applicationContext)
    }
    var previewImageUrl by remember(image, ext) {
        mutableStateOf(buildNmbThumbImageUrl(image = image, ext = ext))
    }
    var hasTriedApiFallback by remember(image, ext) { mutableStateOf(false) }

    fun fetchFallbackCdnIfNeeded() {
        if (hasTriedApiFallback) {
            return
        }
        hasTriedApiFallback = true
        scope.launch {
            val fallbackPreviewImageUrl = resolveNmbImageFallbackUrl(
                repository = repository,
                currentUrl = previewImageUrl
            ) { fallbackCdnBaseUrl ->
                buildNmbThumbImageUrl(
                    image = image,
                    ext = ext,
                    cdnBaseUrl = fallbackCdnBaseUrl
                )
            } ?: return@launch
            previewImageUrl = fallbackPreviewImageUrl
        }
    }

    val resolvedPreviewUrl = previewImageUrl ?: return
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(resolvedPreviewUrl)
            .crossfade(true)
            .build(),
        onError = { _: AsyncImagePainter.State.Error ->
            fetchFallbackCdnIfNeeded()
        }
    )
    val drawable = remember(painter.state) {
        (painter.state as? AsyncImagePainter.State.Success)
            ?.result
            ?.drawable
    }
    val imageSize = remember(drawable) {
        val resolvedDrawable = drawable ?: return@remember null
        val width = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.width
            else -> resolvedDrawable.intrinsicWidth
        }.takeIf { it > 0 } ?: return@remember null
        val height = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.height
            else -> resolvedDrawable.intrinsicHeight
        }.takeIf { it > 0 } ?: return@remember null
        IntSize(width = width, height = height)
    }
    val previewHeightPx = with(density) { THREAD_IMAGE_PREVIEW_HEIGHT_DP.dp.toPx() }
    val minimumPreviewWidthPx = with(density) { THREAD_IMAGE_PREVIEW_MIN_WIDTH_DP.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val maximumPreviewWidthPx = constraints.maxWidth.toFloat()
            .takeIf { it > 0f }
            ?: minimumPreviewWidthPx
        val previewLayout = remember(
            imageSize,
            previewHeightPx,
            minimumPreviewWidthPx,
            maximumPreviewWidthPx
        ) {
            calculateThreadImagePreviewLayout(
                imageSize = imageSize,
                previewHeightPx = previewHeightPx,
                minimumPreviewWidthPx = minimumPreviewWidthPx,
                maximumPreviewWidthPx = maximumPreviewWidthPx
            )
        }

        Surface(
            modifier = Modifier
                .width(with(density) { previewLayout.widthPx.toDp() })
                .height(THREAD_IMAGE_PREVIEW_HEIGHT_DP.dp)
                .clickable { onImageClick(image, ext) },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(THREAD_IMAGE_PREVIEW_CORNER_RADIUS_DP.dp)
        ) {
            Image(
                painter = painter,
                contentDescription = "串图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = previewLayout.alignment
            )
        }
    }
}

private fun calculateThreadImagePreviewLayout(
    imageSize: IntSize?,
    previewHeightPx: Float,
    minimumPreviewWidthPx: Float,
    maximumPreviewWidthPx: Float
): ThreadImagePreviewLayout {
    val resolvedMaximumWidthPx = maximumPreviewWidthPx
        .takeIf { it > 0f }
        ?: minimumPreviewWidthPx
    val resolvedMinimumWidthPx = min(minimumPreviewWidthPx, resolvedMaximumWidthPx)
    val intrinsicSize = imageSize
    if (intrinsicSize == null || intrinsicSize.width <= 0 || intrinsicSize.height <= 0) {
        return ThreadImagePreviewLayout(
            widthPx = min(previewHeightPx, resolvedMaximumWidthPx),
            alignment = Alignment.CenterStart
        )
    }

    val widthAtTargetHeightPx =
        intrinsicSize.width.toFloat() * (previewHeightPx / intrinsicSize.height.toFloat())

    return when {
        widthAtTargetHeightPx > resolvedMaximumWidthPx -> {
            ThreadImagePreviewLayout(
                widthPx = resolvedMaximumWidthPx,
                alignment = Alignment.Center
            )
        }

        widthAtTargetHeightPx < resolvedMinimumWidthPx -> {
            ThreadImagePreviewLayout(
                widthPx = resolvedMinimumWidthPx,
                alignment = Alignment.TopStart
            )
        }

        else -> {
            ThreadImagePreviewLayout(
                widthPx = widthAtTargetHeightPx,
                alignment = Alignment.CenterStart
            )
        }
    }
}
