package ink.duo3.fogisland.ui.components.post

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.material3.MaterialTheme
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.util.NmbLinkTarget

internal data class ThreadPostFocusHighlight(
    val postId: Long,
    val triggerToken: Long
)

@Composable
internal fun ThreadDetailPostItem(
    post: NmbPost,
    onImageClick: (String, String?) -> Unit,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null,
    referencePreviewState: NmbPostReferencePreviewState? = null,
    highlight: ThreadPostFocusHighlight? = null
) {
    NmbPostFlatItem(
        post = post,
        onImageClick = onImageClick,
        onLinkClick = onLinkClick,
        referencePreviewState = referencePreviewState,
        modifier = Modifier.threadPostFocusHighlight(post.remoteId, highlight)
    )
}

private fun Modifier.threadPostFocusHighlight(
    postId: Long,
    highlight: ThreadPostFocusHighlight?
): Modifier = composed {
    val isTarget = highlight?.postId == postId
    val highlightAlpha = remember(highlight?.triggerToken) { Animatable(0f) }

    LaunchedEffect(highlight?.triggerToken, isTarget) {
        if (!isTarget) {
            highlightAlpha.snapTo(0f)
            return@LaunchedEffect
        }

        repeat(2) {
            highlightAlpha.animateTo(0.28f, tween(180, easing = LinearEasing))
            highlightAlpha.animateTo(0.08f, tween(180, easing = LinearEasing))
        }
        highlightAlpha.animateTo(0.28f, tween(180))
        highlightAlpha.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
    }

    background(
        color = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha.value)
    )
}
