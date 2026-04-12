package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ResolvedPostReference
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.network.api.toErrorPresentation
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.preview.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.post.NmbPostReferencePreviewState
import ink.duo3.fogisland.ui.components.post.ReferenceDialogState
import ink.duo3.fogisland.ui.components.post.ReferenceJumpBackFab
import ink.duo3.fogisland.ui.components.post.ReferenceJumpUserScrollLockMillis
import ink.duo3.fogisland.ui.components.post.ReferencePostDialog
import ink.duo3.fogisland.ui.components.post.ThreadDetailPostItem
import ink.duo3.fogisland.ui.components.post.ThreadPostFocusHighlight
import ink.duo3.fogisland.ui.components.post.ThreadReferenceAnchor
import ink.duo3.fogisland.ui.components.post.animateCollapsedFor
import ink.duo3.fogisland.ui.components.post.animateScrollToItemAtPreferredOffset
import ink.duo3.fogisland.ui.components.post.logReferenceJump
import ink.duo3.fogisland.ui.components.post.scrollToItemAtPreferredOffset
import ink.duo3.fogisland.ui.components.post.syncTopAppBarToCurrentListPosition
import ink.duo3.fogisland.ui.components.preview.NmbPreviewSamples
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ThreadDetailScreen(
    detail: ThreadDetail,
    forumName: String?,
    loadedPage: Int,
    isLoading: Boolean,
    canLoadMore: Boolean,
    error: ErrorPresentation?,
    focusPostId: Long? = null,
    focusPage: Int? = null,
    onBack: () -> Unit,
    onReply: () -> Unit,
    onSubscribe: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProgressChanged: (Int, Int) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onResolveCachedPostReference: suspend (Long) -> ResolvedPostReference?,
    onQueryPostReference: suspend (Long, Long?) -> ResolvedPostReference?,
    onLoadCurrentThreadUntilPost: suspend (Long) -> Boolean,
    onLinkClick: (NmbLinkTarget) -> Unit
) {
    val thread = detail.thread
    val posts = detail.posts
    val progress = detail.progress
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val errorOffset = if (error != null) 1 else 0

    var restoredFocusKey by remember { mutableStateOf<Triple<Long?, Long?, Int?>?>(null) }
    var restoredProgressThreadId by remember { mutableStateOf<Long?>(null) }
    var pendingInThreadJump by remember(thread?.id) {
        mutableStateOf<PendingInThreadJump?>(null)
    }
    var nextJumpToken by remember(thread?.id) { mutableStateOf(0L) }
    var referenceJumpScrollLockToken by remember(thread?.id) { mutableStateOf(0L) }
    var referenceJumpUserScrollLocked by remember(thread?.id) { mutableStateOf(false) }
    var focusHighlight by remember(thread?.id) { mutableStateOf<ThreadPostFocusHighlight?>(null) }
    var nextHighlightToken by remember(thread?.id) { mutableStateOf(0L) }
    var referenceDialogState by remember(thread?.id) { mutableStateOf<ReferenceDialogState?>(null) }
    var suppressReferenceJumpFab by remember(thread?.id) { mutableStateOf(false) }
    val referenceJumpStack = remember(thread?.id) { mutableStateListOf<ThreadReferenceAnchor>() }
    val referenceLookupStates = remember(thread?.id) {
        mutableStateMapOf<Long, CachedReferenceLookupState>()
    }
    val latestReferenceJumpUserScrollLocked by rememberUpdatedState(referenceJumpUserScrollLocked)
    val referenceJumpUserScrollBlocker = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (
                    latestReferenceJumpUserScrollLocked &&
                    source == NestedScrollSource.UserInput
                ) {
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (latestReferenceJumpUserScrollLocked) {
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val currentThreadPostsById = remember(thread, posts) {
        buildMap {
            thread?.let { root -> put(root.remoteId, root) }
            posts.forEach { post -> put(post.remoteId, post) }
        }
    }
    val resolvedLookupPostsById = buildMap {
        referenceLookupStates.values.forEach { state ->
            val resolved = state as? CachedReferenceLookupState.Resolved ?: return@forEach
            put(resolved.reference.post.remoteId, resolved.reference.post)
        }
    }
    val resolvedPostsById = remember(currentThreadPostsById, resolvedLookupPostsById) {
        buildMap {
            putAll(currentThreadPostsById)
            putAll(resolvedLookupPostsById)
        }
    }
    val referencePreviewState = NmbPostReferencePreviewState(
        resolvedPosts = resolvedPostsById,
        onImageClick = onImageClick
    )
    val locatedFocusPostIndex = remember(posts, focusPostId) {
        focusPostId?.let { postId ->
            posts.indexOfFirst { it.id == postId || it.remoteId == postId }.takeIf { it >= 0 }
        }
    }
    val shouldSuppressProgressRestore = focusPage != null || locatedFocusPostIndex != null

    fun resolveTargetIndexByPostId(postId: Long): Int? {
        val currentThread = thread ?: return null
        return when {
            currentThread.id == postId || currentThread.remoteId == postId -> errorOffset
            else -> posts.indexOfFirst { it.id == postId || it.remoteId == postId }
                .takeIf { it >= 0 }
                ?.let { errorOffset + 1 + it }
        }
    }

    fun resolvePostIdByIndex(index: Int): Long? {
        val threadPost = thread ?: return null
        return when {
            error != null && index == 0 -> null
            index == errorOffset -> threadPost.remoteId
            else -> posts
                .getOrNull(index - errorOffset - 1)
                ?.remoteId
        }
    }

    val focusTargetIndex = remember(thread?.id, posts, focusPostId, focusPage, errorOffset) {
        focusPostId
            ?.let(::resolveTargetIndexByPostId)
            ?: focusPage?.let { page ->
                posts.indexOfFirst { it.page == page }
                    .takeIf { it >= 0 }
                    ?.let { errorOffset + 1 + it }
            }
    }
    val pendingJumpTargetIndex = remember(thread?.id, posts, pendingInThreadJump, errorOffset) {
        pendingInThreadJump?.targetPostId?.let(::resolveTargetIndexByPostId)
    }

    fun rememberCurrentPositionForJumpBack(sourcePostId: Long) {
        referenceJumpStack += ThreadReferenceAnchor(
            sourcePostId = sourcePostId,
            itemIndex = listState.firstVisibleItemIndex,
            itemOffset = listState.firstVisibleItemScrollOffset
        )
        logReferenceJump {
            "pushBackAnchor source=$sourcePostId " +
                    "anchor=${listState.firstVisibleItemIndex}/${listState.firstVisibleItemScrollOffset} " +
                    "stackSize=${referenceJumpStack.size}"
        }
    }

    fun jumpToInThreadPost(sourcePostId: Long, postId: Long) {
        val previousPendingJump = pendingInThreadJump
        logReferenceJump {
            "jumpToInThreadPost request source=$sourcePostId target=$postId " +
                    "currentPending=$previousPendingJump"
        }
        if (
            previousPendingJump != null &&
            referenceJumpStack.lastOrNull()?.sourcePostId == previousPendingJump.sourcePostId &&
            referenceJumpStack.lastOrNull()?.sourceHasLeftViewport == false
        ) {
            referenceJumpStack.removeAt(referenceJumpStack.lastIndex)
            logReferenceJump {
                "dropPendingAnchor source=${previousPendingJump.sourcePostId} " +
                        "stackSize=${referenceJumpStack.size}"
            }
        }
        val jumpToken = ++nextJumpToken
        referenceJumpScrollLockToken = jumpToken
        referenceJumpUserScrollLocked = true
        coroutineScope.launch {
            delay(ReferenceJumpUserScrollLockMillis)
            if (referenceJumpScrollLockToken == jumpToken) {
                referenceJumpUserScrollLocked = false
                logReferenceJump { "unlockUserScroll brief token=$jumpToken" }
            }
        }
        rememberCurrentPositionForJumpBack(sourcePostId)
        pendingInThreadJump = PendingInThreadJump(
            sourcePostId = sourcePostId,
            targetPostId = postId,
            token = jumpToken
        )
    }

    fun showFocusHighlight(postId: Long) {
        val triggerToken = ++nextHighlightToken
        focusHighlight = ThreadPostFocusHighlight(
            postId = postId,
            triggerToken = triggerToken
        )
        coroutineScope.launch {
            delay(1_800)
            if (focusHighlight?.triggerToken == triggerToken) {
                focusHighlight = null
            }
        }
    }

    fun showResolvedReferenceDialog(reference: ResolvedPostReference) {
        referenceDialogState = ReferenceDialogState.Resolved(reference)
    }

    suspend fun handlePostReferenceClick(sourcePostId: Long, postId: Long) {
        val currentThread = thread ?: return
        val clickMark = TimeSource.Monotonic.markNow()
        logReferenceJump {
            "postReferenceClick source=$sourcePostId target=$postId " +
                    "thread=${currentThread.id} first=${listState.firstVisibleItemIndex}/${listState.firstVisibleItemScrollOffset}"
        }
        val directIndex = resolveTargetIndexByPostId(postId)
        if (directIndex != null) {
            logReferenceJump { "postReferenceClick directInThread target=$postId index=$directIndex" }
            jumpToInThreadPost(sourcePostId, postId)
            return
        }

        val currentLookup = referenceLookupStates[postId]
        val cachedResolved = (currentLookup as? CachedReferenceLookupState.Resolved)?.reference
        if (cachedResolved != null) {
            if (cachedResolved.threadId == currentThread.id) {
                logReferenceJump { "postReferenceClick cachedSameThread target=$postId" }
                jumpToInThreadPost(sourcePostId, postId)
            } else {
                logReferenceJump {
                    "postReferenceClick cachedCrossThread target=$postId thread=${cachedResolved.threadId}"
                }
                showResolvedReferenceDialog(cachedResolved)
            }
            return
        }

        referenceDialogState = ReferenceDialogState.Loading(postId)
        logReferenceJump {
            "postReferenceClick queryReference start target=$postId preferredThread=null"
        }
        val queryReferenceMark = TimeSource.Monotonic.markNow()
        val resolved = try {
            onQueryPostReference(postId, null)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val error = throwable.toErrorPresentation("加载引用目标失败")
            logReferenceJump {
                "postReferenceClick queryReference error target=$postId " +
                    "summary=${error.summary} detail=${error.detail} " +
                    "elapsed=${queryReferenceMark.elapsedNow()} totalElapsed=${clickMark.elapsedNow()}"
            }
            referenceDialogState = ReferenceDialogState.Error(
                postId = postId,
                message = error.toReferenceDialogMessage()
            )
            return
        }
        logReferenceJump {
            "postReferenceClick queryReference end target=$postId " +
                "hit=${resolved != null} thread=${resolved?.threadId} " +
                "elapsed=${queryReferenceMark.elapsedNow()} totalElapsed=${clickMark.elapsedNow()}"
        }
        if (resolved != null) {
            referenceLookupStates[postId] = CachedReferenceLookupState.Resolved(resolved)
            if (resolved.threadId == currentThread.id) {
                logReferenceJump { "postReferenceClick querySameThread target=$postId" }
                jumpToInThreadPost(sourcePostId, postId)
                referenceDialogState = null
            } else {
                logReferenceJump {
                    "postReferenceClick queryCrossThread target=$postId thread=${resolved.threadId}"
                }
                showResolvedReferenceDialog(resolved)
            }
        } else {
            logReferenceJump { "postReferenceClick miss target=$postId" }
            referenceLookupStates[postId] = CachedReferenceLookupState.Miss
            referenceDialogState = ReferenceDialogState.Error(
                postId = postId,
                message = "未找到引用内容"
            )
        }
    }

    fun observePostReference(postId: Long) {
        if (postId in resolvedPostsById || postId in referenceLookupStates) {
            return
        }
        referenceLookupStates[postId] = CachedReferenceLookupState.Loading
        coroutineScope.launch {
            val resolved = onResolveCachedPostReference(postId)
            if (resolved != null) {
                referenceLookupStates[postId] = CachedReferenceLookupState.Resolved(resolved)
            } else {
                referenceLookupStates.remove(postId)
            }
        }
    }

    val handleNonPostLinkClick = remember(onLinkClick) {
        { target: NmbLinkTarget ->
            if (target !is NmbLinkTarget.PostReference) {
                onLinkClick(target)
            }
        }
    }

    LaunchedEffect(thread?.id, focusPostId, focusPage, focusTargetIndex) {
        val threadId = thread?.id ?: return@LaunchedEffect
        if (focusPostId == null && focusPage == null) {
            return@LaunchedEffect
        }

        val focusKey = Triple(threadId, focusPostId, focusPage)
        if (restoredFocusKey == focusKey) {
            return@LaunchedEffect
        }

        val targetIndex = focusTargetIndex ?: return@LaunchedEffect

        focusHighlight = null
        listState.scrollToItemAtPreferredOffset(
            index = targetIndex.coerceAtLeast(0),
            topAppBarScrollBehavior = topAppBarScrollBehavior
        )
        showFocusHighlight(focusPostId ?: threadId)
        restoredFocusKey = focusKey
    }

    LaunchedEffect(thread?.id, pendingInThreadJump, pendingJumpTargetIndex) {
        val jump = pendingInThreadJump ?: return@LaunchedEffect
        val targetIndex = pendingJumpTargetIndex ?: return@LaunchedEffect
        logReferenceJump {
            "pendingJump effect start source=${jump.sourcePostId} target=${jump.targetPostId} " +
                    "index=$targetIndex first=${listState.firstVisibleItemIndex}/${listState.firstVisibleItemScrollOffset} " +
                    "visible=${listState.layoutInfo.visibleItemsInfo.joinToString { item -> item.index.toString() }}"
        }
        try {
            focusHighlight = null
            val scrollCompleted = listState.animateScrollToItemAtPreferredOffset(
                index = targetIndex.coerceAtLeast(0),
                topAppBarScrollBehavior = topAppBarScrollBehavior
            )
            if (!scrollCompleted) {
                referenceJumpStack.lastOrNull()
                    ?.takeIf { it.sourcePostId == jump.sourcePostId && !it.sourceHasLeftViewport }
                    ?.let {
                        referenceJumpStack.removeAt(referenceJumpStack.lastIndex)
                        logReferenceJump {
                            "pendingJump removeAnchorBecauseScrollIncomplete " +
                                    "source=${jump.sourcePostId} stackSize=${referenceJumpStack.size}"
                        }
                    }
                return@LaunchedEffect
            }
            referenceJumpStack.lastOrNull()
                ?.takeIf { it.sourcePostId == jump.sourcePostId }
                ?.let { anchor ->
                    val visiblePostIds = listState.layoutInfo.visibleItemsInfo
                        .mapNotNull { item -> resolvePostIdByIndex(item.index) }
                        .toSet()
                    logReferenceJump {
                        "pendingJump afterScroll source=${jump.sourcePostId} " +
                                "target=${jump.targetPostId} sourceVisible=" +
                                "${jump.sourcePostId in visiblePostIds} visiblePosts=$visiblePostIds"
                    }
                    if (jump.sourcePostId in visiblePostIds) {
                        referenceJumpStack.removeAt(referenceJumpStack.lastIndex)
                        logReferenceJump {
                            "pendingJump removeAnchorBecauseSourceVisible " +
                                    "source=${jump.sourcePostId} stackSize=${referenceJumpStack.size}"
                        }
                    } else {
                        referenceJumpStack[referenceJumpStack.lastIndex] =
                            anchor.copy(sourceHasLeftViewport = true)
                        logReferenceJump {
                            "pendingJump markSourceLeft source=${jump.sourcePostId} " +
                                    "stackSize=${referenceJumpStack.size}"
                        }
                    }
                }
            showFocusHighlight(jump.targetPostId)
        } finally {
            logReferenceJump { "pendingJump effect end target=${jump.targetPostId} token=${jump.token}" }
            if (pendingInThreadJump?.token == jump.token) {
                pendingInThreadJump = null
            }
        }
    }

    LaunchedEffect(thread?.id, posts.size, progress?.threadId) {
        if (shouldSuppressProgressRestore) {
            return@LaunchedEffect
        }
        val threadId = thread?.id ?: return@LaunchedEffect
        val readProgress = progress ?: return@LaunchedEffect
        if (restoredProgressThreadId == threadId) {
            return@LaunchedEffect
        }

        val targetIndex = when (val postId = readProgress.lastReadPostId) {
            null -> readProgress.lastVisibleItemIndex
            else -> resolveTargetIndexByPostId(postId) ?: readProgress.lastVisibleItemIndex
        }

        listState.scrollToItem(
            index = targetIndex.coerceAtLeast(0),
            scrollOffset = readProgress.lastVisibleItemOffset
        )
        listState.syncTopAppBarToCurrentListPosition(topAppBarScrollBehavior)
        restoredProgressThreadId = threadId
    }

    LaunchedEffect(thread?.id, posts.size) {
        if (thread == null) {
            return@LaunchedEffect
        }

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .map { it.first to it.second }
            .distinctUntilChanged()
            .debounce(300)
            .collect { (index, offset) ->
                onProgressChanged(index, offset)
            }
    }

    LaunchedEffect(
        thread?.id,
        posts.size,
        errorOffset,
        referenceJumpStack.size,
        pendingInThreadJump
    ) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { item -> resolvePostIdByIndex(item.index) }
                .toSet()
        }
            .distinctUntilChanged()
            .collect { visiblePostIds ->
                val pendingJump = pendingInThreadJump
                referenceJumpStack.lastOrNull()
                    ?.takeIf { anchor ->
                        pendingJump?.sourcePostId == anchor.sourcePostId &&
                            !anchor.sourceHasLeftViewport &&
                            anchor.sourcePostId !in visiblePostIds
                    }
                    ?.let { anchor ->
                        referenceJumpStack[referenceJumpStack.lastIndex] =
                            anchor.copy(sourceHasLeftViewport = true)
                        logReferenceJump {
                            "visibleObserver markSourceLeft source=${anchor.sourcePostId} " +
                                "visiblePosts=$visiblePostIds"
                        }
                    }

                while (
                    referenceJumpStack.isNotEmpty() &&
                    referenceJumpStack.last().sourceHasLeftViewport &&
                    referenceJumpStack.last().sourcePostId in visiblePostIds
                ) {
                    logReferenceJump {
                        "visibleObserver popAnchor source=${referenceJumpStack.last().sourcePostId} " +
                                "visiblePosts=$visiblePostIds"
                    }
                    referenceJumpStack.removeAt(referenceJumpStack.lastIndex)
                }
            }
    }

    referenceDialogState?.let { dialogState ->
        ReferencePostDialog(
            state = dialogState,
            onDismiss = { referenceDialogState = null },
            onOpenThread = { resolved ->
                referenceDialogState = null
                onLinkClick(
                    NmbLinkTarget.Thread(
                        threadId = resolved.threadId,
                        targetPostId = resolved.post
                            .takeIf { !it.isThread }
                            ?.remoteId
                    )
                )
            },
            onImageClick = onImageClick
        )
    }

    val currentAnchor = referenceJumpStack.lastOrNull()
    val shouldShowReferenceJumpFab =
        !suppressReferenceJumpFab && currentAnchor?.sourceHasLeftViewport == true

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        floatingActionButton = {
            ReferenceJumpBackFab(
                anchor = currentAnchor,
                visible = shouldShowReferenceJumpFab,
                onClick = { fabAnchor ->
                    focusHighlight = null
                    if (referenceJumpStack.lastOrNull() == fabAnchor) {
                        referenceJumpStack.removeAt(referenceJumpStack.lastIndex)
                    }
                    suppressReferenceJumpFab = true
                    coroutineScope.launch {
                        try {
                            coroutineScope {
                                val appBarJob = launch {
                                    topAppBarScrollBehavior.animateCollapsedFor(
                                        listState = listState,
                                        collapsed = fabAnchor.itemIndex > 0 ||
                                            fabAnchor.itemOffset > 0
                                    )
                                }
                                listState.animateScrollToItem(
                                    index = fabAnchor.itemIndex,
                                    scrollOffset = fabAnchor.itemOffset
                                )
                                appBarJob.join()
                            }
                            listState.syncTopAppBarToCurrentListPosition(topAppBarScrollBehavior)
                        } finally {
                            suppressReferenceJumpFab = false
                        }
                    }
                }
            )
        },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            thread?.let { currentThread ->
                                currentThread.title
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "串 No.${currentThread.id}"
                            } ?: "加载中…"
                        )
                        forumName?.takeIf { it.isNotBlank() }?.let { boardName ->
                            Text(
                                text = boardName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        thread?.let {
                            Text(
                                text = "已加载到第 $loadedPage 页",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onReply,
                        enabled = thread != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "回帖")
                    }
                    IconButton(
                        onClick = onSubscribe,
                        enabled = thread != null
                    ) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "订阅")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(referenceJumpUserScrollBlocker),
                state = listState,
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                error?.let { errorState ->
                    item {
                        ErrorMessageCard(
                            error = errorState,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                thread?.let { root ->
                    item(key = "thread-${root.id}") {
                        ThreadDetailPostItem(
                            post = root,
                            onImageClick = onImageClick,
                            onLinkClick = handleNonPostLinkClick,
                            referencePreviewState = referencePreviewState.copy(
                                onReferenceObserved = ::observePostReference,
                                onPostReferenceClick = { sourcePostId, postId ->
                                    coroutineScope.launch {
                                        handlePostReferenceClick(sourcePostId, postId)
                                    }
                                }
                            ),
                            highlight = focusHighlight
                        )
                    }
                }

                items(
                    items = posts,
                    key = { post -> "${post.threadId}-${post.id}" }
                ) { post ->
                    ThreadDetailPostItem(
                        post = post,
                        onImageClick = onImageClick,
                        onLinkClick = handleNonPostLinkClick,
                        referencePreviewState = referencePreviewState.copy(
                            onReferenceObserved = ::observePostReference,
                            onPostReferenceClick = { sourcePostId, postId ->
                                coroutineScope.launch {
                                    handlePostReferenceClick(sourcePostId, postId)
                                }
                            }
                        ),
                        highlight = focusHighlight
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Button(
                            onClick = onLoadMore,
                            enabled = canLoadMore && !isLoading
                        ) {
                            Text(
                                when {
                                    isLoading -> "加载中…"
                                    canLoadMore -> "加载更多回复"
                                    else -> "已加载全部回复"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PendingInThreadJump(
    val sourcePostId: Long,
    val targetPostId: Long,
    val token: Long
)

private fun ErrorPresentation.toReferenceDialogMessage(): String {
    val detailText = detail?.takeIf { it.isNotBlank() && it != summary }
    return detailText?.let { "$summary\n$it" } ?: summary
}

private sealed interface CachedReferenceLookupState {
    data object Loading : CachedReferenceLookupState
    data object Miss : CachedReferenceLookupState
    data class Resolved(val reference: ResolvedPostReference) : CachedReferenceLookupState
}

@Preview(name = "Thread Detail Items", widthDp = 412, heightDp = 900)
@Composable
private fun ThreadDetailPostItemPreview() {
    FogIslandPreviewColumn(verticalSpacingDp = 0) {
        ThreadDetailPostItem(
            post = NmbPreviewSamples.forumThread,
            onImageClick = { _, _ -> },
            onLinkClick = null
        )
        ThreadDetailPostItem(
            post = NmbPreviewSamples.replyPost,
            onImageClick = { _, _ -> },
            onLinkClick = null
        )
    }
}

@Preview(name = "Thread Detail Screen", widthDp = 412, heightDp = 960)
@Composable
private fun ThreadDetailScreenPreview() {
    FogIslandPreviewColumn {
        ThreadDetailScreen(
            detail = ThreadDetail(
                thread = NmbPreviewSamples.forumThread,
                posts = listOf(NmbPreviewSamples.replyPost),
                progress = null
            ),
            forumName = "欢乐恶搞",
            loadedPage = 2,
            isLoading = false,
            canLoadMore = true,
            error = null,
            onBack = {},
            onReply = {},
            onSubscribe = {},
            onRefresh = {},
            onLoadMore = {},
            onProgressChanged = { _, _ -> },
            onImageClick = { _, _ -> },
            onResolveCachedPostReference = { null },
            onQueryPostReference = { _, _ -> null },
            onLoadCurrentThreadUntilPost = { false },
            onLinkClick = {}
        )
    }
}
