package ink.duo3.fogisland.ui.components.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ViewerTopBarScrim = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.78f),
        Color.Black.copy(alpha = 0.34f),
        Color.Transparent
    )
)

private const val TOP_BAR_VISIBILITY_ANIMATION_MILLIS = 240

@Composable
internal fun BoxScope.ImageViewerTopBar(
    visible: Boolean,
    canHandleImageFile: Boolean,
    isDownloadingImage: Boolean,
    isSharingImage: Boolean,
    showDownloadingProgress: Boolean,
    showSharingProgress: Boolean,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopCenter),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = TOP_BAR_VISIBILITY_ANIMATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(
                durationMillis = TOP_BAR_VISIBILITY_ANIMATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = TOP_BAR_VISIBILITY_ANIMATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        ) + slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(
                durationMillis = TOP_BAR_VISIBILITY_ANIMATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(152.dp)
                    .background(ViewerTopBarScrim)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.24f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = "图片",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canHandleImageFile) {
                    ViewerActionButton(
                        enabled = !isSharingImage && !isDownloadingImage,
                        inProgress = showDownloadingProgress,
                        onClick = onDownload
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "下载图片",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ViewerActionButton(
                        enabled = !isSharingImage && !isDownloadingImage,
                        inProgress = showSharingProgress,
                        onClick = onShare
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "分享图片",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerActionButton(
    enabled: Boolean,
    inProgress: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.24f),
        shape = CircleShape
    ) {
        IconButton(
            enabled = enabled,
            onClick = onClick
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                content()
            }
        }
    }
}
