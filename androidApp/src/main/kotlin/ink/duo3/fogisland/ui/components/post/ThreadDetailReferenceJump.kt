package ink.duo3.fogisland.ui.components.post

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import ink.duo3.fogisland.utils.DebugLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val ReferenceJumpUserScrollSettleMillis = 80L
internal const val ReferenceJumpUserScrollLockMillis = 240L

private const val ReferenceJumpScrollTimeoutMillis = 2_000L
private const val ReferenceJumpAppBarTimeoutMillis = 500L
private const val ThreadReferenceJumpLogTag = "ThreadRefJump"

@OptIn(ExperimentalMaterial3Api::class)
internal fun TopAppBarScrollBehavior.setCollapsedFor(
    listState: LazyListState,
    collapsed: Boolean
) {
    val resolvedCollapsed = collapsed && listState.canCollapseTopAppBar()
    if (resolvedCollapsed) {
        state.heightOffset = state.heightOffsetLimit
        state.contentOffset = state.contentOffset.coerceAtLeast(1f)
    } else {
        state.heightOffset = 0f
        state.contentOffset = 0f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun TopAppBarScrollBehavior.animateCollapsedFor(
    listState: LazyListState,
    collapsed: Boolean
) {
    val resolvedCollapsed = collapsed && listState.canCollapseTopAppBar()
    val targetHeightOffset = if (resolvedCollapsed) state.heightOffsetLimit else 0f
    val currentHeightOffset = state.heightOffset
    if (abs(currentHeightOffset - targetHeightOffset) < 0.5f) {
        if (resolvedCollapsed) {
            state.contentOffset = state.contentOffset.coerceAtLeast(1f)
        } else {
            state.contentOffset = 0f
        }
        return
    }

    val heightOffset = Animatable(currentHeightOffset)
    heightOffset.updateBounds(
        lowerBound = state.heightOffsetLimit,
        upperBound = 0f
    )
    heightOffset.animateTo(
        targetValue = targetHeightOffset,
        animationSpec = tween(
            durationMillis = if (collapsed) 220 else 180,
            easing = FastOutSlowInEasing
        )
    ) {
        state.heightOffset = value
    }
    if (resolvedCollapsed) {
        state.contentOffset = state.contentOffset.coerceAtLeast(1f)
    } else {
        state.contentOffset = 0f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun LazyListState.syncTopAppBarToCurrentListPosition(
    topAppBarScrollBehavior: TopAppBarScrollBehavior
) {
    if (!canCollapseTopAppBar()) {
        topAppBarScrollBehavior.setCollapsedFor(this, collapsed = false)
        return
    }
    val isAtAbsoluteTop = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    topAppBarScrollBehavior.setCollapsedFor(this, collapsed = !isAtAbsoluteTop)
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun LazyListState.animateScrollToItemAtPreferredOffset(
    index: Int,
    topAppBarScrollBehavior: TopAppBarScrollBehavior
): Boolean {
    val resolvedIndex = index.coerceAtLeast(0)
    val jumpScrollOffset = preferredReferenceJumpScrollOffset()
    logReferenceJump {
        "animateScroll start targetIndex=$resolvedIndex " +
            "offset=$jumpScrollOffset " +
            "first=$firstVisibleItemIndex/$firstVisibleItemScrollOffset"
    }
    val completed = coroutineScope {
        val appBarJob = launch {
            topAppBarScrollBehavior.animateCollapsedFor(
                listState = this@animateScrollToItemAtPreferredOffset,
                collapsed = resolvedIndex > 0
            )
        }
        val scrollCompleted = animateReferenceJumpScroll(
            index = resolvedIndex,
            scrollOffset = jumpScrollOffset
        )
        val appBarCompleted = withTimeoutOrNull(ReferenceJumpAppBarTimeoutMillis) {
            appBarJob.join()
            true
        } == true
        if (!appBarCompleted) {
            appBarJob.cancel()
            logReferenceJump { "animateScroll appBar timeout targetIndex=$resolvedIndex" }
        }
        scrollCompleted
    }
    if (!completed) {
        logReferenceJump {
            "animateScroll incomplete targetIndex=$resolvedIndex " +
                "first=$firstVisibleItemIndex/$firstVisibleItemScrollOffset"
        }
        return false
    }
    syncTopAppBarToCurrentListPosition(topAppBarScrollBehavior)
    logReferenceJump {
        "animateScroll end targetIndex=$resolvedIndex " +
            "first=$firstVisibleItemIndex/$firstVisibleItemScrollOffset"
    }
    return true
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun LazyListState.scrollToItemAtPreferredOffset(
    index: Int,
    topAppBarScrollBehavior: TopAppBarScrollBehavior
) {
    val resolvedIndex = index.coerceAtLeast(0)
    scrollToItem(
        index = resolvedIndex,
        scrollOffset = preferredReferenceJumpScrollOffset()
    )
    syncTopAppBarToCurrentListPosition(topAppBarScrollBehavior)
}

internal fun logReferenceJump(message: () -> String) {
    DebugLog.d(ThreadReferenceJumpLogTag, message)
}

private fun LazyListState.canCollapseTopAppBar(): Boolean {
    return canScrollForward || canScrollBackward
}

private fun LazyListState.preferredReferenceJumpScrollOffset(): Int {
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    if (viewportHeight <= 0) {
        return 0
    }
    return -(viewportHeight * 0.25f).roundToInt()
}

private suspend fun LazyListState.animateReferenceJumpScroll(
    index: Int,
    scrollOffset: Int
): Boolean {
    suspend fun animateOnce(): Boolean {
        return withTimeoutOrNull(ReferenceJumpScrollTimeoutMillis) {
            animateScrollToItem(index = index, scrollOffset = scrollOffset)
            true
        } == true
    }

    return try {
        animateOnce()
    } catch (cancellation: CancellationException) {
        val isStillActive = currentCoroutineContext().isActive
        logReferenceJump {
            "animateScroll cancelled targetIndex=$index active=$isStillActive " +
                "reason=${cancellation.javaClass.simpleName}"
        }
        if (!isStillActive) {
            throw cancellation
        }
        delay(ReferenceJumpUserScrollSettleMillis)
        try {
            val retryCompleted = animateOnce()
            logReferenceJump { "animateScroll retry targetIndex=$index completed=$retryCompleted" }
            retryCompleted
        } catch (retryCancellation: CancellationException) {
            if (!currentCoroutineContext().isActive) {
                throw retryCancellation
            }
            logReferenceJump {
                "animateScroll retryCancelled targetIndex=$index " +
                    "reason=${retryCancellation.javaClass.simpleName}"
            }
            false
        }
    }
}
