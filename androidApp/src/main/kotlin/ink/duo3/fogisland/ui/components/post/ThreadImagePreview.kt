package ink.duo3.fogisland.ui.components.post

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.util.buildNmbThumbImageUrl
import ink.duo3.fogisland.ui.components.preview.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.preview.NmbPreviewImages
import ink.duo3.fogisland.utils.resolveNmbImageFallbackUrl
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val THREAD_IMAGE_PREVIEW_HEIGHT_DP = 128
private const val THREAD_IMAGE_PREVIEW_MIN_WIDTH_DP = 64
private const val THREAD_IMAGE_PREVIEW_CORNER_RADIUS_DP = 8

private data class ThreadImagePreviewLayout(
    val widthPx: Float,
    val alignment: Alignment
)

private data class ThreadImagePreviewSpec(
    val imageSize: IntSize,
    val brush: Brush
)

@Composable
fun ThreadImagePreview(
    image: String,
    ext: String?,
    onImageClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    previewHeight: Dp = THREAD_IMAGE_PREVIEW_HEIGHT_DP.dp,
    minimumPreviewWidth: Dp = THREAD_IMAGE_PREVIEW_MIN_WIDTH_DP.dp,
    cornerRadius: Dp = THREAD_IMAGE_PREVIEW_CORNER_RADIUS_DP.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val inspectionMode = LocalInspectionMode.current
    val previewSpec = remember(image) { resolveThreadImagePreviewSpec(image) }
    val previewHeightPx = with(density) { previewHeight.toPx() }
    val minimumPreviewWidthPx = with(density) { minimumPreviewWidth.toPx() }

    if (inspectionMode && previewSpec != null) {
        BoxWithConstraints(modifier = modifier) {
            val maximumPreviewWidthPx = constraints.maxWidth.toFloat()
                .takeIf { it > 0f }
                ?: minimumPreviewWidthPx
            val previewLayout = remember(
                previewSpec,
                previewHeightPx,
                minimumPreviewWidthPx,
                maximumPreviewWidthPx
            ) {
                calculateThreadImagePreviewLayout(
                    imageSize = previewSpec.imageSize,
                    previewHeightPx = previewHeightPx,
                    minimumPreviewWidthPx = minimumPreviewWidthPx,
                    maximumPreviewWidthPx = maximumPreviewWidthPx
                )
            }
            val cropScale = max(
                previewLayout.widthPx / previewSpec.imageSize.width.toFloat(),
                previewHeightPx / previewSpec.imageSize.height.toFloat()
            )
            val contentWidthPx = previewSpec.imageSize.width * cropScale
            val contentHeightPx = previewSpec.imageSize.height * cropScale

            Surface(
                modifier = Modifier
                    .width(with(density) { previewLayout.widthPx.toDp() })
                    .height(previewHeight)
                    .clickable { onImageClick(image, ext) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(cornerRadius)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(previewLayout.alignment)
                            .width(with(density) { contentWidthPx.toDp() })
                            .height(with(density) { contentHeightPx.toDp() })
                            .background(previewSpec.brush)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .size(18.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(width = 34.dp, height = 10.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }
        }
        return
    }

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
                .height(previewHeight)
                .clickable { onImageClick(image, ext) },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(cornerRadius)
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

private fun resolveThreadImagePreviewSpec(image: String): ThreadImagePreviewSpec? {
    return when (image) {
        NmbPreviewImages.Normal -> ThreadImagePreviewSpec(
            imageSize = IntSize(width = 1280, height = 1280),
            brush = Brush.linearGradient(
                listOf(Color(0xFF7FB3FF), Color(0xFFB8E0D2))
            )
        )

        NmbPreviewImages.Wide -> ThreadImagePreviewSpec(
            imageSize = IntSize(width = 2200, height = 1080),
            brush = Brush.linearGradient(
                listOf(Color(0xFFF5B971), Color(0xFFE57C73))
            )
        )

        NmbPreviewImages.Tall -> ThreadImagePreviewSpec(
            imageSize = IntSize(width = 720, height = 2200),
            brush = Brush.linearGradient(
                listOf(Color(0xFF9BC1BC), Color(0xFF5D576B))
            )
        )

        NmbPreviewImages.Long -> ThreadImagePreviewSpec(
            imageSize = IntSize(width = 720, height = 4200),
            brush = Brush.linearGradient(
                listOf(Color(0xFF9AD1D4), Color(0xFF102542))
            )
        )

        else -> null
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

@Preview(name = "Thread Image Preview", widthDp = 412, heightDp = 560)
@Composable
private fun ThreadImagePreviewGalleryPreview() {
    FogIslandPreviewColumn {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("宽图")
            ThreadImagePreview(
                image = NmbPreviewImages.Wide,
                ext = "jpg",
                onImageClick = { _, _ -> }
            )
            Text("正常图")
            ThreadImagePreview(
                image = NmbPreviewImages.Normal,
                ext = "jpg",
                onImageClick = { _, _ -> }
            )
            Text("窄长图")
            ThreadImagePreview(
                image = NmbPreviewImages.Tall,
                ext = "jpg",
                onImageClick = { _, _ -> }
            )
        }
    }
}
