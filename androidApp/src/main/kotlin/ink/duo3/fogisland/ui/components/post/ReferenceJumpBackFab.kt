package ink.duo3.fogisland.ui.components.post

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ReferenceJumpFabSize = 56.dp
private val ReferenceJumpFabShadowInset = 28.dp
private val ReferenceJumpFabShadowCanvasSize = ReferenceJumpFabSize + ReferenceJumpFabShadowInset * 2

internal data class ThreadReferenceAnchor(
    val sourcePostId: Long,
    val itemIndex: Int,
    val itemOffset: Int,
    val sourceHasLeftViewport: Boolean = false
)

@Composable
internal fun ReferenceJumpBackFab(
    anchor: ThreadReferenceAnchor?,
    visible: Boolean,
    onClick: (ThreadReferenceAnchor) -> Unit
) {
    var displayedAnchor by remember { mutableStateOf<ThreadReferenceAnchor?>(null) }
    LaunchedEffect(visible, anchor) {
        if (visible) {
            displayedAnchor = anchor
        }
    }

    val enterEasing = remember { CubicBezierEasing(0f, 0f, 0f, 1f) }
    val exitEasing = remember { CubicBezierEasing(1f, 0f, 1f, 1f) }
    val alphaSpec = tween<Float>(
        durationMillis = if (visible) 210 else 140,
        easing = if (visible) enterEasing else exitEasing
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = alphaSpec,
        label = "referenceJumpFabAlpha"
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 180 else 120,
            easing = if (visible) enterEasing else exitEasing
        ),
        label = "referenceJumpFabShadowAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 18.dp,
        animationSpec = tween<Dp>(
            durationMillis = if (visible) 260 else 160,
            easing = if (visible) enterEasing else exitEasing
        ),
        label = "referenceJumpFabOffsetY"
    )

    LaunchedEffect(visible, alpha) {
        if (!visible && alpha <= 0.01f) {
            displayedAnchor = null
        }
    }

    val fabAnchor = displayedAnchor
    if (
        fabAnchor == null ||
        (!visible && alpha <= 0.01f && shadowAlpha <= 0.01f)
    ) {
        return
    }

    Box(
        modifier = Modifier.offset(
            x = ReferenceJumpFabShadowInset,
            y = offsetY + ReferenceJumpFabShadowInset
        ),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier.size(ReferenceJumpFabShadowCanvasSize),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(shadowAlpha)
                    .drawBehind {
                        val fabSize = ReferenceJumpFabSize.toPx()
                        val shadowInset = ReferenceJumpFabShadowInset.toPx()
                        val cornerRadius = 16.dp.toPx()
                        val shadowLeft = shadowInset
                        val shadowTop = shadowInset + 4.dp.toPx()
                        val shadowRect = RectF(
                            shadowLeft,
                            shadowTop,
                            shadowLeft + fabSize,
                            shadowTop + fabSize
                        )

                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val ambientPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.argb(30, 0, 0, 0)
                                maskFilter = BlurMaskFilter(
                                    18.dp.toPx(),
                                    BlurMaskFilter.Blur.NORMAL
                                )
                            }
                            nativeCanvas.drawRoundRect(
                                shadowRect,
                                cornerRadius,
                                cornerRadius,
                                ambientPaint
                            )

                            val keyPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.argb(22, 0, 0, 0)
                                maskFilter = BlurMaskFilter(
                                    8.dp.toPx(),
                                    BlurMaskFilter.Blur.NORMAL
                                )
                            }
                            val keyOffset = 3.dp.toPx()
                            shadowRect.offset(0f, keyOffset)
                            nativeCanvas.drawRoundRect(
                                shadowRect,
                                cornerRadius,
                                cornerRadius,
                                keyPaint
                            )
                        }
                    }
            )
            FloatingActionButton(
                onClick = {
                    if (visible) {
                        onClick(fabAnchor)
                    }
                },
                modifier = Modifier
                    .offset(
                        x = -ReferenceJumpFabShadowInset,
                        y = -ReferenceJumpFabShadowInset
                    )
                    .alpha(alpha),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "返回引用处"
                )
            }
        }
    }
}
