package ink.duo3.fogisland.ui.components.imageviewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val LOADING_INDICATOR_GLOBAL_ROTATION_TARGET = 1080f
private const val LOADING_INDICATOR_MIN_PROGRESS = 0.1f
private const val LOADING_INDICATOR_MAX_PROGRESS = 0.87f
private const val LOADING_INDICATOR_ANIMATION_DURATION_MILLIS = 6000
private const val LOADING_PROGRESS_UPDATE_MILLIS = 80
private const val LOADING_INDICATOR_SIZE_DP = 40
private const val LOADING_INDICATOR_CONTAINER_SIZE_DP = 72
private const val LOADING_INDICATOR_STROKE_WIDTH_DP = 4

private val LoadingIndicatorTrackColor = Color.White.copy(alpha = 0.18f)
private val LoadingIndicatorBackgroundColor = Color.Black.copy(alpha = 0.42f)
private val LoadingIndicatorProgressEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
internal fun ImageViewerLoadingIndicator(
    progressFraction: Float?,
    modifier: Modifier = Modifier
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
    var hasDeterminateProgress by remember { mutableStateOf(false) }

    LaunchedEffect(progressFraction) {
        if (progressFraction == null) {
            hasDeterminateProgress = false
            return@LaunchedEffect
        }

        val targetProgress = progressFraction.coerceIn(0f, 1f)
        if (!hasDeterminateProgress) {
            determinateProgress.snapTo(
                indeterminateProgress.coerceIn(
                    LOADING_INDICATOR_MIN_PROGRESS,
                    LOADING_INDICATOR_MAX_PROGRESS
                ).coerceAtLeast(targetProgress)
            )
            hasDeterminateProgress = true
        }

        val monotonicTargetProgress = targetProgress.coerceAtLeast(determinateProgress.value)
        if (monotonicTargetProgress <= determinateProgress.value + 0.001f) {
            return@LaunchedEffect
        }

        determinateProgress.animateTo(
            targetValue = monotonicTargetProgress,
            animationSpec = tween(
                durationMillis = LOADING_PROGRESS_UPDATE_MILLIS,
                easing = FastOutSlowInEasing
            )
        )
    }

    val displayedProgress = if (hasDeterminateProgress) {
        determinateProgress.value.coerceIn(0f, 1f)
    } else {
        indeterminateProgress.coerceIn(
            LOADING_INDICATOR_MIN_PROGRESS,
            LOADING_INDICATOR_MAX_PROGRESS
        )
    }

    Box(
        modifier = modifier
            .size(LOADING_INDICATOR_CONTAINER_SIZE_DP.dp)
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
