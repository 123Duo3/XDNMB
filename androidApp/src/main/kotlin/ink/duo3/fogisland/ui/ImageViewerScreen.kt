package ink.duo3.fogisland.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.ViewConfiguration
import android.widget.OverScroller
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size as CoilSize
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.util.buildNmbFullImageUrl
import ink.duo3.fogisland.utils.HttpProgressInterceptor
import ink.duo3.fogisland.utils.HttpTransferProgress
import ink.duo3.fogisland.utils.ImageDownloadResult
import ink.duo3.fogisland.utils.downloadBitmapImage
import ink.duo3.fogisland.utils.shareBitmapImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.CircleShape
import ink.duo3.fogisland.ui.components.imageviewer.IMAGE_VIEWER_MAX_SCALE
import ink.duo3.fogisland.ui.components.imageviewer.IMAGE_VIEWER_MIN_SCALE
import ink.duo3.fogisland.ui.components.imageviewer.IMAGE_VIEWER_DOUBLE_TAP_SCALE_MULTIPLIER
import ink.duo3.fogisland.ui.components.imageviewer.ImageViewerLayout
import ink.duo3.fogisland.ui.components.imageviewer.ImageViewerTopBar
import ink.duo3.fogisland.ui.components.imageviewer.calculateImageViewerLayout
import ink.duo3.fogisland.ui.components.imageviewer.calculateImageViewerMinimumScale
import ink.duo3.fogisland.ui.components.imageviewer.calculateImageViewerResetTranslation
import ink.duo3.fogisland.ui.components.imageviewer.calculateImageViewerScaledTranslation
import ink.duo3.fogisland.ui.components.imageviewer.calculateImageViewerTranslationBounds
import ink.duo3.fogisland.ui.components.imageviewer.clampImageViewerTranslation
import ink.duo3.fogisland.ui.components.imageviewer.coerceImageViewerGestureScale
import ink.duo3.fogisland.ui.components.imageviewer.coerceImageViewerSettledScale

private val ViewerBackground = Color.Black
private const val MAX_LONG_IMAGE_WIDTH_DP = 600
private const val DOUBLE_TAP_ANIMATION_MILLIS = 220
private const val SCALE_RESET_THRESHOLD = 0.05f
private const val ACTION_PROGRESS_DELAY_MILLIS = 150L
private const val LOADING_INDICATOR_SHOW_DELAY_MILLIS = 120L
private const val LOADING_PROGRESS_COMPLETION_HOLD_MILLIS = 240L
private const val LOADING_INDICATOR_FADE_OUT_MILLIS = 180
private const val LOADING_PROGRESS_FULL_FRACTION = 1f
private const val LOADING_INDICATOR_GLOBAL_ROTATION_TARGET = 1080f
private const val LOADING_INDICATOR_MIN_PROGRESS = 0.1f
private const val LOADING_INDICATOR_MAX_PROGRESS = 0.87f
private const val LOADING_INDICATOR_ANIMATION_DURATION_MILLIS = 6000
private const val LOADING_INDICATOR_SIZE_DP = 40
private const val LOADING_INDICATOR_CONTAINER_SIZE_DP = 72
private const val LOADING_INDICATOR_STROKE_WIDTH_DP = 4
private val LoadingIndicatorTrackColor = Color.White.copy(alpha = 0.18f)
private val LoadingIndicatorBackgroundColor = Color.Black.copy(alpha = 0.42f)
private val LoadingIndicatorProgressEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun ImageViewerScreen(
    image: String,
    ext: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val baseImageLoader = context.imageLoader
    val overScroller = remember(context, image, ext) { OverScroller(context) }
    val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val repository = remember(context) {
        RepositoryProvider.provideForumRepository(context.applicationContext)
    }
    val windowInsetsController = remember(activity, view) {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
        }
    }

    var imageUrl by remember(image, ext) {
        mutableStateOf(buildNmbFullImageUrl(image = image, ext = ext))
    }
    var hasRequestedFallback by remember(image, ext) { mutableStateOf(false) }
    var isFetchingFallback by remember(image, ext) { mutableStateOf(false) }
    var hasTerminalError by remember(image, ext) { mutableStateOf(imageUrl == null) }
    var viewportSize by remember(image, ext) { mutableStateOf(IntSize.Zero) }
    var scale by remember(image, ext) { mutableFloatStateOf(IMAGE_VIEWER_MIN_SCALE) }
    var translation by remember(image, ext) { mutableStateOf(Offset.Zero) }
    var animationJob by remember(image, ext) { mutableStateOf<Job?>(null) }
    var hasUserInteracted by remember(image, ext) { mutableStateOf(false) }
    var lastGestureAnchor by remember(image, ext) { mutableStateOf<Offset?>(null) }
    var controlsVisible by remember(image, ext) { mutableStateOf(true) }
    var isSharingImage by remember(image, ext) { mutableStateOf(false) }
    var isDownloadingImage by remember(image, ext) { mutableStateOf(false) }
    var showSharingProgress by remember(image, ext) { mutableStateOf(false) }
    var showDownloadingProgress by remember(image, ext) { mutableStateOf(false) }
    var shouldShowLoadingIndicator by remember(image, ext) { mutableStateOf(true) }
    var shouldCompleteLoadingIndicator by remember(image, ext) { mutableStateOf(false) }
    val imageLoadProgress = remember(image, ext) { MutableStateFlow<HttpTransferProgress?>(null) }
    val currentImageLoadProgress by imageLoadProgress.collectAsState()
    val progressImageLoader = remember(baseImageLoader, imageUrl) {
        if (imageUrl == null) {
            baseImageLoader
        } else {
            baseImageLoader
                .newBuilder()
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addNetworkInterceptor(
                            HttpProgressInterceptor { progress ->
                                if (progress.url == imageUrl) {
                                    imageLoadProgress.value = progress
                                }
                            }
                        )
                        .build()
                }
                .build()
        }
    }

    DisposableEffect(baseImageLoader, progressImageLoader) {
        onDispose {
            if (progressImageLoader !== baseImageLoader) {
                progressImageLoader.shutdown()
            }
        }
    }

    LaunchedEffect(isSharingImage) {
        if (!isSharingImage) {
            showSharingProgress = false
            return@LaunchedEffect
        }
        delay(ACTION_PROGRESS_DELAY_MILLIS)
        showSharingProgress = true
    }

    LaunchedEffect(isDownloadingImage) {
        if (!isDownloadingImage) {
            showDownloadingProgress = false
            return@LaunchedEffect
        }
        delay(ACTION_PROGRESS_DELAY_MILLIS)
        showDownloadingProgress = true
    }

    LaunchedEffect(imageUrl) {
        shouldShowLoadingIndicator = false
        shouldCompleteLoadingIndicator = false
        imageLoadProgress.value = imageUrl?.let { trackedUrl ->
            HttpTransferProgress(
                url = trackedUrl,
                bytesRead = 0L,
                totalBytes = -1L
            )
        }
    }

    fun resolveDisplayedScale(layout: ImageViewerLayout?): Float {
        return if (layout != null && !hasUserInteracted && animationJob == null) {
            layout.resetScale
        } else {
            scale
        }
    }

    fun resolveDisplayedTranslation(layout: ImageViewerLayout?): Offset {
        return if (layout != null && !hasUserInteracted && animationJob == null) {
            calculateImageViewerResetTranslation(layout)
        } else {
            translation
        }
    }

    fun animateTransform(
        targetScale: Float,
        targetTranslation: Offset,
        useTween: Boolean,
        initialScale: Float = scale,
        initialTranslation: Offset = translation
    ) {
        overScroller.forceFinished(true)
        animationJob?.cancel()
        val startScale = initialScale
        val startTranslation = initialTranslation
        if (
            abs(startScale - targetScale) < 0.001f &&
            abs(startTranslation.x - targetTranslation.x) < 0.5f &&
            abs(startTranslation.y - targetTranslation.y) < 0.5f
        ) {
            scale = targetScale
            translation = targetTranslation
            return
        }

        animationJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = if (useTween) {
                    tween(
                        durationMillis = DOUBLE_TAP_ANIMATION_MILLIS,
                        easing = FastOutSlowInEasing
                    )
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }
            ) { value, _ ->
                scale = lerp(startScale, targetScale, value)
                translation = Offset(
                    x = lerp(startTranslation.x, targetTranslation.x, value),
                    y = lerp(startTranslation.y, targetTranslation.y, value)
                )
            }
            scale = targetScale
            translation = targetTranslation
        }
    }

    fun settleTransform(
        layout: ImageViewerLayout,
        minimumScale: Float,
        useTween: Boolean = false
    ) {
        val currentScale = resolveDisplayedScale(layout)
        val currentTranslation = resolveDisplayedTranslation(layout)
        val settledScale = coerceImageViewerSettledScale(currentScale, minimumScale)
        val rebasedTranslation = if (abs(settledScale - currentScale) > 0.001f) {
            val settleAnchor = lastGestureAnchor ?: Offset(
                x = layout.viewportSize.width / 2f,
                y = layout.viewportSize.height / 2f
            )
            calculateImageViewerScaledTranslation(
                currentTranslation = currentTranslation,
                currentScale = currentScale,
                targetScale = settledScale,
                anchor = settleAnchor,
                destinationAnchor = settleAnchor
            )
        } else {
            currentTranslation
        }
        val settledTranslation = clampImageViewerTranslation(
            translation = rebasedTranslation,
            layout = layout,
            scale = settledScale
        )
        animateTransform(
            targetScale = settledScale,
            targetTranslation = settledTranslation,
            useTween = useTween,
            initialScale = currentScale,
            initialTranslation = currentTranslation
        )
    }

    fun startSinglePointerFling(
        layout: ImageViewerLayout,
        minimumScale: Float,
        velocityTracker: VelocityTracker
    ): Boolean {
        val currentScale = resolveDisplayedScale(layout)
        val currentTranslation = resolveDisplayedTranslation(layout)
        val settledScale = coerceImageViewerSettledScale(currentScale, minimumScale)
        if (abs(currentScale - settledScale) > 0.001f) {
            return false
        }

        val velocity = velocityTracker.calculateVelocity()
        val bounds = calculateImageViewerTranslationBounds(
            layout = layout,
            scale = currentScale
        )
        val minimumFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
        val maximumFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity.toFloat()
        val flingVelocityX = velocity.x
            .takeIf { abs(it) >= minimumFlingVelocity && bounds.minX < bounds.maxX }
            ?.coerceIn(-maximumFlingVelocity, maximumFlingVelocity)
            ?: 0f
        val flingVelocityY = velocity.y
            .takeIf { abs(it) >= minimumFlingVelocity && bounds.minY < bounds.maxY }
            ?.coerceIn(-maximumFlingVelocity, maximumFlingVelocity)
            ?: 0f
        if (flingVelocityX == 0f && flingVelocityY == 0f) {
            return false
        }

        overScroller.forceFinished(true)
        animationJob?.cancel()
        scale = currentScale
        translation = currentTranslation
        animationJob = scope.launch {
            overScroller.fling(
                currentTranslation.x.roundToInt(),
                currentTranslation.y.roundToInt(),
                flingVelocityX.roundToInt(),
                flingVelocityY.roundToInt(),
                bounds.minX.roundToInt(),
                bounds.maxX.roundToInt(),
                bounds.minY.roundToInt(),
                bounds.maxY.roundToInt()
            )
            while (overScroller.computeScrollOffset()) {
                translation = Offset(
                    x = overScroller.currX.toFloat(),
                    y = overScroller.currY.toFloat()
                )
                withFrameNanos { }
            }
            translation = clampImageViewerTranslation(
                translation = translation,
                layout = layout,
                scale = currentScale
            )
        }
        return true
    }

    fun fetchFallbackCdnIfNeeded() {
        if (hasRequestedFallback || isFetchingFallback || imageUrl == null) {
            return
        }
        hasRequestedFallback = true
        isFetchingFallback = true
        hasTerminalError = false
        scope.launch {
            val fallbackBaseUrl = runCatching {
                repository.getImageCdnFallbackBaseUrl()
            }.getOrNull()
            val fallbackUrl = fallbackBaseUrl?.let { baseUrl ->
                buildNmbFullImageUrl(
                    image = image,
                    ext = ext,
                    cdnBaseUrl = baseUrl
                )
            }

            if (fallbackUrl.isNullOrBlank() || fallbackUrl == imageUrl) {
                hasTerminalError = true
            } else {
                imageUrl = fallbackUrl
            }
            isFetchingFallback = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            overScroller.forceFinished(true)
            animationJob?.cancel()
        }
    }

    DisposableEffect(windowInsetsController) {
        val controller = windowInsetsController ?: return@DisposableEffect onDispose { }
        val previousLightStatusBars = controller.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller.isAppearanceLightNavigationBars
        val previousBehavior = controller.systemBarsBehavior
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = previousLightStatusBars
            controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            controller.systemBarsBehavior = previousBehavior
        }
    }

    LaunchedEffect(windowInsetsController, controlsVisible) {
        val controller = windowInsetsController ?: return@LaunchedEffect
        if (controlsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    val imageRequest = remember(imageUrl) {
        imageUrl?.let { resolvedUrl ->
            ImageRequest.Builder(context)
                .data(resolvedUrl)
                .size(CoilSize.ORIGINAL)
                .precision(Precision.EXACT)
                .crossfade(false)
                .build()
        }
    }
    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = progressImageLoader
    )
    val painterState = painter.state
    val currentSuccessState = remember(imageUrl, painterState) {
        (painterState as? AsyncImagePainter.State.Success)
            ?.takeIf { state ->
                state.result.request.data == imageUrl
            }
    }
    val sourceBitmap = remember(currentSuccessState) {
        val drawable = currentSuccessState
            ?.result
            ?.drawable
            ?: return@remember null
        (drawable as? BitmapDrawable)?.bitmap?.also { bitmap ->
            bitmap.density = Bitmap.DENSITY_NONE
        }
    }
    val imageBitmap = remember(sourceBitmap) {
        sourceBitmap?.asImageBitmap()
    }
    val imageSize = remember(currentSuccessState) {
        val drawable = currentSuccessState
            ?.result
            ?.drawable
            ?: return@remember null
        val width = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.width
            else -> drawable.intrinsicWidth
        }.takeIf { it > 0 } ?: return@remember null
        val height = when (drawable) {
            is BitmapDrawable -> drawable.bitmap.height
            else -> drawable.intrinsicHeight
        }.takeIf { it > 0 } ?: return@remember null
        IntSize(width, height)
    }
    val maxLongImageWidthPx = with(density) { MAX_LONG_IMAGE_WIDTH_DP.dp.toPx() }
    val layout = remember(viewportSize, imageSize, maxLongImageWidthPx) {
        calculateImageViewerLayout(
            viewportSize = viewportSize,
            imageSize = imageSize,
            maxLongImageWidthPx = maxLongImageWidthPx
        )
    }
    val minimumScale = remember(layout, imageSize) {
        layout?.let { currentLayout ->
            calculateImageViewerMinimumScale(
                layout = currentLayout
            )
        } ?: IMAGE_VIEWER_MIN_SCALE
    }
    val canHandleImageFile = sourceBitmap != null && imageUrl != null

    LaunchedEffect(imageUrl, currentSuccessState, hasTerminalError) {
        if (currentSuccessState != null || hasTerminalError) {
            shouldShowLoadingIndicator = false
            return@LaunchedEffect
        }
        delay(LOADING_INDICATOR_SHOW_DELAY_MILLIS)
        shouldShowLoadingIndicator = true
    }

    LaunchedEffect(imageUrl, painterState, currentSuccessState) {
        when {
            currentSuccessState != null -> {
                hasTerminalError = false
                isFetchingFallback = false
            }

            painterState is AsyncImagePainter.State.Error -> {
                imageLoadProgress.value = null
                shouldShowLoadingIndicator = false
                shouldCompleteLoadingIndicator = false
                if (!hasRequestedFallback) {
                    fetchFallbackCdnIfNeeded()
                } else {
                    hasTerminalError = true
                    isFetchingFallback = false
                }
            }
        }
    }
    LaunchedEffect(currentSuccessState, imageUrl) {
        if (currentSuccessState == null) {
            return@LaunchedEffect
        }
        val resolvedUrl = imageUrl ?: return@LaunchedEffect
        shouldCompleteLoadingIndicator = true
        imageLoadProgress.value = HttpTransferProgress(
            url = resolvedUrl,
            bytesRead = 1L,
            totalBytes = 1L
        )
    }

    LaunchedEffect(layout, minimumScale) {
        val currentLayout = layout ?: return@LaunchedEffect
        overScroller.forceFinished(true)
        animationJob?.cancel()
        if (!hasUserInteracted) {
            scale = currentLayout.resetScale
            translation = calculateImageViewerResetTranslation(currentLayout)
            return@LaunchedEffect
        }

        scale = coerceImageViewerSettledScale(scale, minimumScale)
        translation = clampImageViewerTranslation(
            translation = translation,
            layout = currentLayout,
            scale = scale
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBackground)
    ) {
        viewportSize = IntSize(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(layout, minimumScale) {
                    val currentLayout = layout ?: return@pointerInput
                    awaitEachGesture {
                        val velocityTracker = VelocityTracker()
                        var didTransform = false
                        var gestureHadMultiTouch = false
                        var shouldEndGesture = false
                        do {
                            val event = awaitPointerEvent()
                            val pressedPointerCount = event.changes.count { it.pressed }
                            if (pressedPointerCount >= 2) {
                                gestureHadMultiTouch = true
                            } else if (gestureHadMultiTouch) {
                                shouldEndGesture = true
                                break
                            } else {
                                event.changes
                                    .firstOrNull { it.pressed }
                                    ?.let { change ->
                                        velocityTracker.addPosition(
                                            timeMillis = change.uptimeMillis,
                                            position = change.position
                                        )
                                    }
                            }
                            val previousCentroid = event.calculateCentroid(useCurrent = false)
                            val currentCentroid = event.calculateCentroid(useCurrent = true)
                            val zoomChange = event.calculateZoom()
                            val hasGestureChange =
                                previousCentroid != Offset.Unspecified &&
                                    currentCentroid != Offset.Unspecified &&
                                    (zoomChange != 1f || currentCentroid != previousCentroid)
                            if (hasGestureChange) {
                                val currentScale = if (didTransform) {
                                    scale
                                } else {
                                    resolveDisplayedScale(currentLayout)
                                }
                                val currentTranslation = if (didTransform) {
                                    translation
                                } else {
                                    resolveDisplayedTranslation(currentLayout)
                                }
                                if (!didTransform) {
                                    overScroller.forceFinished(true)
                                    animationJob?.cancel()
                                    scale = currentScale
                                    translation = currentTranslation
                                    hasUserInteracted = true
                                }
                                didTransform = true
                                lastGestureAnchor = currentCentroid
                                val targetScale = coerceImageViewerGestureScale(
                                    scale = currentScale * zoomChange,
                                    minimumScale = minimumScale
                                )
                                val rawTranslation = calculateImageViewerScaledTranslation(
                                    currentTranslation = currentTranslation,
                                    currentScale = currentScale,
                                    targetScale = targetScale,
                                    anchor = previousCentroid,
                                    destinationAnchor = currentCentroid
                                )
                                translation = if (gestureHadMultiTouch) {
                                    rawTranslation
                                } else {
                                    clampImageViewerTranslation(
                                        translation = rawTranslation,
                                        layout = currentLayout,
                                        scale = targetScale
                                    )
                                }
                                scale = targetScale
                                event.changes
                                    .filter { it.positionChanged() }
                                    .forEach { it.consume() }
                            }
                        } while (!shouldEndGesture && event.changes.any { it.pressed })

                        if (didTransform) {
                            val startedFling = !gestureHadMultiTouch && startSinglePointerFling(
                                layout = currentLayout,
                                minimumScale = minimumScale,
                                velocityTracker = velocityTracker
                            )
                            if (!startedFling) {
                                settleTransform(
                                    layout = currentLayout,
                                    minimumScale = minimumScale
                                )
                            }
                        }
                    }
                }
                .pointerInput(layout, minimumScale, controlsVisible) {
                    val currentLayout = layout ?: return@pointerInput
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { tapOffset ->
                            val currentScale = resolveDisplayedScale(currentLayout)
                            val currentTranslation = resolveDisplayedTranslation(currentLayout)
                            animationJob?.cancel()
                            scale = currentScale
                            translation = currentTranslation
                            hasUserInteracted = true
                            if (abs(currentScale - currentLayout.resetScale) > SCALE_RESET_THRESHOLD) {
                                val targetTranslation = if (currentLayout.isLongImage) {
                                    clampImageViewerTranslation(
                                        translation = calculateImageViewerScaledTranslation(
                                            currentTranslation = currentTranslation,
                                            currentScale = currentScale,
                                            targetScale = currentLayout.resetScale,
                                            anchor = tapOffset,
                                            destinationAnchor = Offset(
                                                x = tapOffset.x,
                                                y = currentLayout.viewportSize.height / 2f
                                            )
                                        ),
                                        layout = currentLayout,
                                        scale = currentLayout.resetScale
                                    )
                                } else {
                                    calculateImageViewerResetTranslation(currentLayout)
                                }
                                animateTransform(
                                    targetScale = currentLayout.resetScale,
                                    targetTranslation = targetTranslation,
                                    useTween = true,
                                    initialScale = currentScale,
                                    initialTranslation = currentTranslation
                                )
                                return@detectTapGestures
                            }

                            val targetScale = (currentLayout.resetScale *
                                IMAGE_VIEWER_DOUBLE_TAP_SCALE_MULTIPLIER)
                                .coerceAtLeast(currentLayout.resetScale)
                                .coerceAtMost(IMAGE_VIEWER_MAX_SCALE)
                            val targetTranslation = clampImageViewerTranslation(
                                translation = calculateImageViewerScaledTranslation(
                                    currentTranslation = currentTranslation,
                                    currentScale = currentScale,
                                    targetScale = targetScale,
                                    anchor = tapOffset,
                                    destinationAnchor = tapOffset
                                ),
                                layout = currentLayout,
                                scale = targetScale
                            )
                            animateTransform(
                                targetScale = targetScale,
                                targetTranslation = targetTranslation,
                                useTween = true,
                                initialScale = currentScale,
                                initialTranslation = currentTranslation
                            )
                        }
                    )
                }
        ) {
            if (layout != null && currentSuccessState != null) {
                val currentLayout = layout
                val displayedScale = resolveDisplayedScale(currentLayout)
                val displayedTranslation = resolveDisplayedTranslation(currentLayout)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    withTransform({
                        translate(
                            left = displayedTranslation.x,
                            top = displayedTranslation.y
                        )
                        scale(
                            scaleX = displayedScale,
                            scaleY = displayedScale,
                            pivot = Offset.Zero
                        )
                    }) {
                        imageBitmap?.let { bitmap ->
                            drawImage(
                                image = bitmap
                            )
                        } ?: with(painter) {
                            draw(
                                size = Size(
                                    width = currentLayout.baseSize.width,
                                    height = currentLayout.baseSize.height
                                )
                            )
                        }
                    }
                }
            }

            when {
                hasTerminalError -> {
                    Text(
                        text = "图片加载失败",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                !hasTerminalError && shouldShowLoadingIndicator -> {
                    val progressFraction = currentImageLoadProgress
                        ?.takeIf { it.url == imageUrl }
                        ?.fraction
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        ImageViewerLoadingIndicator(
                            progressFraction = progressFraction,
                            shouldComplete = shouldCompleteLoadingIndicator,
                            onDismissed = {
                                shouldCompleteLoadingIndicator = false
                                imageLoadProgress.value = null
                                shouldShowLoadingIndicator = false
                            }
                        )
                    }
                }
            }
        }

        ImageViewerTopBar(
            visible = controlsVisible,
            canHandleImageFile = canHandleImageFile,
            isDownloadingImage = isDownloadingImage,
            isSharingImage = isSharingImage,
            showDownloadingProgress = showDownloadingProgress,
            showSharingProgress = showSharingProgress,
            onBack = onBack,
            onDownload = {
                val bitmap = sourceBitmap ?: return@ImageViewerTopBar
                val resolvedUrl = imageUrl ?: return@ImageViewerTopBar
                isDownloadingImage = true
                scope.launch {
                    val result = downloadBitmapImage(
                        context = context,
                        bitmap = bitmap,
                        imageId = image,
                        ext = ext,
                        imageUrl = resolvedUrl
                    )
                    isDownloadingImage = false
                    result.onSuccess { downloadResult ->
                        val message = when (downloadResult) {
                            is ImageDownloadResult.Saved -> "图片已保存到相册"
                            is ImageDownloadResult.Enqueued -> "已加入系统下载队列"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShare = {
                val bitmap = sourceBitmap ?: return@ImageViewerTopBar
                val resolvedUrl = imageUrl ?: return@ImageViewerTopBar
                isSharingImage = true
                scope.launch {
                    val result = shareBitmapImage(
                        context = context,
                        bitmap = bitmap,
                        imageId = image,
                        ext = ext,
                        imageUrl = resolvedUrl
                    )
                    isSharingImage = false
                    result.onSuccess { shareIntent ->
                        context.startActivity(
                            Intent.createChooser(shareIntent, "分享图片")
                        )
                    }.onFailure {
                        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
private fun ImageViewerLoadingIndicator(
    progressFraction: Float?,
    shouldComplete: Boolean,
    onDismissed: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "image_loading_indicator")
    val globalRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = LOADING_INDICATOR_GLOBAL_ROTATION_TARGET,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOADING_INDICATOR_ANIMATION_DURATION_MILLIS,
                easing = LinearEasing
            )
        ),
        label = "image_loading_global_rotation"
    )
    val indeterminateProgress by transition.animateFloat(
        initialValue = LOADING_INDICATOR_MIN_PROGRESS,
        targetValue = LOADING_INDICATOR_MAX_PROGRESS,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = LOADING_INDICATOR_ANIMATION_DURATION_MILLIS
                LOADING_INDICATOR_MAX_PROGRESS at
                    (LOADING_INDICATOR_ANIMATION_DURATION_MILLIS / 2) using
                    LoadingIndicatorProgressEasing
                LOADING_INDICATOR_MIN_PROGRESS at LOADING_INDICATOR_ANIMATION_DURATION_MILLIS
            }
        ),
        label = "image_loading_indeterminate_progress"
    )
    val determinateProgress = remember { Animatable(LOADING_INDICATOR_MIN_PROGRESS) }
    val indicatorAlpha = remember { Animatable(1f) }
    var hasDeterminateProgress by remember { mutableStateOf(false) }
    var completionShown by remember { mutableStateOf(false) }

    LaunchedEffect(progressFraction) {
        if (progressFraction == null) {
            indicatorAlpha.snapTo(1f)
            hasDeterminateProgress = false
            completionShown = false
            return@LaunchedEffect
        }

        val targetProgress = progressFraction.coerceIn(0f, LOADING_PROGRESS_FULL_FRACTION)
        if (!hasDeterminateProgress) {
            determinateProgress.snapTo(
                indeterminateProgress.coerceIn(
                    LOADING_INDICATOR_MIN_PROGRESS,
                    LOADING_INDICATOR_MAX_PROGRESS
                )
            )
            hasDeterminateProgress = true
        }
        determinateProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
    }

    LaunchedEffect(shouldComplete, hasDeterminateProgress, determinateProgress.value) {
        if (
            !shouldComplete ||
            !hasDeterminateProgress ||
            completionShown ||
            determinateProgress.value < 0.999f
        ) {
            return@LaunchedEffect
        }
        completionShown = true
        delay(LOADING_PROGRESS_COMPLETION_HOLD_MILLIS)
        indicatorAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = LOADING_INDICATOR_FADE_OUT_MILLIS,
                easing = FastOutSlowInEasing
            )
        )
        onDismissed()
    }

    val displayedProgress = if (hasDeterminateProgress) {
        determinateProgress.value.coerceIn(0f, LOADING_PROGRESS_FULL_FRACTION)
    } else {
        indeterminateProgress.coerceIn(LOADING_INDICATOR_MIN_PROGRESS, LOADING_INDICATOR_MAX_PROGRESS)
    }
    Box(
        modifier = Modifier
            .size(LOADING_INDICATOR_CONTAINER_SIZE_DP.dp)
            .graphicsLayer {
                alpha = indicatorAlpha.value
            }
            .background(
                color = LoadingIndicatorBackgroundColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { displayedProgress },
            modifier = Modifier
                .size(LOADING_INDICATOR_SIZE_DP.dp)
                .graphicsLayer {
                    rotationZ = globalRotation
                },
            color = Color.White,
            strokeWidth = LOADING_INDICATOR_STROKE_WIDTH_DP.dp,
            trackColor = LoadingIndicatorTrackColor
        )
    }
}
