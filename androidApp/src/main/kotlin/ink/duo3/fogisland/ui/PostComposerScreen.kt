package ink.duo3.fogisland.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.cookieCollectionFlow
import ink.duo3.fogisland.data.draft.deleteDraftImage
import ink.duo3.fogisland.data.draft.persistDraftImage
import ink.duo3.fogisland.data.draft.readDraftImage
import ink.duo3.fogisland.shared.model.CookieCollection
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.PostingDraftEntry
import ink.duo3.fogisland.shared.model.ReplyPostRequest
import ink.duo3.fogisland.shared.model.ThreadPostImage
import ink.duo3.fogisland.shared.model.ThreadPostRequest
import ink.duo3.fogisland.shared.network.api.toErrorPresentation
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.util.isPostImageTooLarge
import ink.duo3.fogisland.ui.components.composer.ActivePostCookieCard
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.composer.PostDraftFields
import ink.duo3.fogisland.ui.components.composer.PostImageCard
import ink.duo3.fogisland.ui.components.composer.hasSamePayloadAs
import ink.duo3.fogisland.ui.components.composer.preparePostImageForSending
import ink.duo3.fogisland.ui.components.composer.readPostImage
import ink.duo3.fogisland.ui.components.composer.shouldPreparePostImageForSending
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TECH_SUPPORT_FORUM_ID = 117L

private data class PostForumOption(
    val id: Long,
    val name: String,
    val groupName: String
)

internal sealed interface PostComposerMode {
    val initialDraft: PostingDraftEntry?
    val isPosting: Boolean
    val error: ErrorPresentation?
    val onClearError: () -> Unit
    val onPreviewImage: (String) -> Unit

    data class Thread(
        val forumGroups: List<ForumGroup>,
        val initialForumId: Long?,
        override val initialDraft: PostingDraftEntry?,
        override val isPosting: Boolean,
        override val error: ErrorPresentation?,
        val onMenuClick: () -> Unit,
        override val onClearError: () -> Unit,
        override val onPreviewImage: (String) -> Unit,
        val onSubmit: (ThreadPostRequest, Long?) -> Unit
    ) : PostComposerMode

    data class Reply(
        val threadId: Long,
        val threadTitle: String?,
        val forumName: String?,
        override val initialDraft: PostingDraftEntry?,
        override val isPosting: Boolean,
        override val error: ErrorPresentation?,
        val onBack: () -> Unit,
        override val onClearError: () -> Unit,
        override val onPreviewImage: (String) -> Unit,
        val onSubmit: (ReplyPostRequest, Long?) -> Unit
    ) : PostComposerMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostComposerScreen(
    mode: PostComposerMode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draftPersistenceScope = remember(context.applicationContext) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    val repository = remember(context) { RepositoryProvider.provideForumRepository(context.applicationContext) }
    val cookieCollection by context.cookieCollectionFlow.collectAsState(
        initial = CookieCollection(
            cookies = emptyList(),
            activeRequestCookieId = null,
            activePostCookieId = null
        )
    )
    val activePostCookie = remember(cookieCollection) {
        cookieCollection.cookies.firstOrNull { it.id == cookieCollection.activePostCookieId }
    }
    val screenKey = remember(mode) {
        when (mode) {
            is PostComposerMode.Thread -> "thread:${mode.initialDraft?.id ?: "new"}:${mode.initialForumId ?: 0L}"
            is PostComposerMode.Reply -> "reply:${mode.initialDraft?.id ?: "new"}:${mode.threadId}"
        }
    }
    val forumOptions = remember(mode) {
        when (mode) {
            is PostComposerMode.Thread -> {
                mode.forumGroups.flatMap { group ->
                    group.forums.map { forum ->
                        PostForumOption(
                            id = forum.id,
                            name = forum.name.ifBlank { forum.displayName },
                            groupName = group.name
                        )
                    }
                }
            }

            is PostComposerMode.Reply -> emptyList()
        }
    }
    val initialSelectedForumId = remember(mode, forumOptions) {
        when (mode) {
            is PostComposerMode.Thread -> {
                mode.initialDraft?.forumId
                    ?.takeIf { forumId -> forumOptions.any { it.id == forumId } }
                    ?: mode.initialForumId
                        ?.takeIf { forumId -> forumOptions.any { it.id == forumId } }
                    ?: forumOptions.firstOrNull { it.id == TECH_SUPPORT_FORUM_ID }?.id
                    ?: mode.initialForumId
                    ?: TECH_SUPPORT_FORUM_ID
            }

            is PostComposerMode.Reply -> null
        }
    }
    var selectedForumId by rememberSaveable(screenKey, initialSelectedForumId) {
        mutableStateOf(initialSelectedForumId ?: TECH_SUPPORT_FORUM_ID)
    }
    var name by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.name.orEmpty()) }
    var email by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.email.orEmpty()) }
    var title by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.title.orEmpty()) }
    var content by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.contentText.orEmpty()) }
    var selectedImage by remember(screenKey) { mutableStateOf<ThreadPostImage?>(null) }
    var draftImagePath by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.imagePath) }
    var draftImageFileName by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.imageFileName) }
    var draftImageMimeType by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.imageMimeType) }
    var useWatermark by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.useWatermark ?: true) }
    var currentDraftId by rememberSaveable(screenKey) { mutableStateOf(mode.initialDraft?.id) }
    var isForumPickerVisible by rememberSaveable(screenKey) { mutableStateOf(false) }
    var hasTriedSubmit by rememberSaveable(screenKey) { mutableStateOf(false) }
    var imageError by remember(screenKey) { mutableStateOf<ErrorPresentation?>(null) }
    var draftError by remember(screenKey) { mutableStateOf<ErrorPresentation?>(null) }
    var isPreparingImage by remember(screenKey) { mutableStateOf(false) }
    var suppressDraftPersistenceOnDispose by remember(screenKey) { mutableStateOf(false) }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val selectedForum = remember(selectedForumId, forumOptions) {
        forumOptions.firstOrNull { it.id == selectedForumId }
    }
    val hasContentOrImage = content.isNotBlank() || selectedImage != null
    val isContentInvalid = hasTriedSubmit && !hasContentOrImage
    val isSelectedImageTooLarge = remember(selectedImage) { selectedImage.isPostImageTooLarge() }
    val canSubmit = !mode.isPosting &&
        !isPreparingImage &&
        !isSelectedImageTooLarge &&
        hasContentOrImage &&
        activePostCookie != null
    val replyMode = mode as? PostComposerMode.Reply
    val displayReplyTitle = remember(replyMode) {
        replyMode?.threadTitle?.takeIf { it.isNotBlank() }?.let { titleText ->
            titleText
        } ?: replyMode?.let { "串 No.${it.threadId}" }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            imageError = null
            runCatching {
                context.readPostImage(uri)
            }.onSuccess { image ->
                mode.onClearError()
                if (draftImagePath != null) {
                    context.deleteDraftImage(draftImagePath)
                    draftImagePath = null
                }
                draftImageFileName = image.fileName
                draftImageMimeType = image.mimeType
                selectedImage = image
            }.onFailure { throwable ->
                imageError = ErrorPresentation(
                    summary = "读取图片失败",
                    detail = throwable.message
                )
            }
        }
    }

    LaunchedEffect(initialSelectedForumId) {
        if (mode is PostComposerMode.Thread && selectedForum == null) {
            selectedForumId = initialSelectedForumId ?: TECH_SUPPORT_FORUM_ID
        }
    }

    LaunchedEffect(screenKey, draftImagePath) {
        val imagePath = draftImagePath ?: return@LaunchedEffect
        if (selectedImage != null) {
            return@LaunchedEffect
        }
        selectedImage = context.readDraftImage(
            path = imagePath,
            fileName = draftImageFileName,
            mimeType = draftImageMimeType
        )
        if (selectedImage == null && draftImagePath == imagePath) {
            draftImagePath = null
            draftImageFileName = null
            draftImageMimeType = null
        } else {
            draftImageFileName = selectedImage?.fileName
            draftImageMimeType = selectedImage?.mimeType
        }
    }

    LaunchedEffect(mode.isPosting, mode.error) {
        if (!mode.isPosting && mode.error != null) {
            suppressDraftPersistenceOnDispose = false
        }
    }

    LaunchedEffect(selectedImage) {
        val currentImage = selectedImage ?: return@LaunchedEffect
        if (isPreparingImage || !shouldPreparePostImageForSending(currentImage)) {
            return@LaunchedEffect
        }

        isPreparingImage = true
        runCatching {
            context.preparePostImageForSending(currentImage)
        }.onSuccess { preparedImage ->
            if (!preparedImage.hasSamePayloadAs(currentImage)) {
                val existingDraftImagePath = draftImagePath
                if (existingDraftImagePath != null) {
                    context.deleteDraftImage(existingDraftImagePath)
                    if (draftImagePath == existingDraftImagePath) {
                        draftImagePath = null
                    }
                }
                draftImageFileName = preparedImage.fileName
                draftImageMimeType = preparedImage.mimeType
                selectedImage = preparedImage
            }
        }.onFailure { throwable ->
            imageError = ErrorPresentation(
                summary = "处理图片失败",
                detail = throwable.message
            )
        }
        isPreparingImage = false
    }

    LaunchedEffect(
        screenKey,
        selectedForumId,
        name,
        email,
        title,
        content,
        selectedImage,
        useWatermark,
        currentDraftId,
        mode.isPosting,
        isPreparingImage
    ) {
        if (mode.isPosting || isPreparingImage) {
            return@LaunchedEffect
        }

        val hasDraftContent = name.isNotEmpty() ||
            email.isNotEmpty() ||
            title.isNotEmpty() ||
            content.isNotEmpty() ||
            selectedImage != null ||
            draftImagePath != null

        delay(800)

        try {
            if (!hasDraftContent) {
                val existingDraftId = currentDraftId ?: return@LaunchedEffect
                repository.deletePostingDraft(existingDraftId)
                context.deleteDraftImage(draftImagePath)
                draftImagePath = null
                currentDraftId = null
                draftError = null
                return@LaunchedEffect
            }

            val currentImage = selectedImage
            val persistedImagePath = if (currentImage != null) {
                draftImagePath ?: context.persistDraftImage(
                    image = currentImage,
                    existingPath = null
                ).also { draftImagePath = it }
            } else {
                null
            }

            currentDraftId = when (mode) {
                is PostComposerMode.Thread -> repository.saveThreadDraft(
                    draftId = currentDraftId,
                    request = ThreadPostRequest(
                        forumId = selectedForumId,
                        name = name,
                        email = email,
                        title = title,
                        content = content,
                        useWatermark = selectedImage != null && useWatermark,
                        image = selectedImage
                    ),
                    imagePath = persistedImagePath
                )

                is PostComposerMode.Reply -> repository.saveReplyDraft(
                    draftId = currentDraftId,
                    request = ReplyPostRequest(
                        threadId = mode.threadId,
                        name = name,
                        email = email,
                        title = title,
                        content = content,
                        useWatermark = selectedImage != null && useWatermark,
                        image = selectedImage
                    ),
                    imagePath = persistedImagePath
                )
            }
            draftError = null
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            draftError = throwable.toErrorPresentation("保存草稿失败")
        }
    }

    val latestSelectedForumId by rememberUpdatedState(selectedForumId)
    val latestName by rememberUpdatedState(name)
    val latestEmail by rememberUpdatedState(email)
    val latestTitle by rememberUpdatedState(title)
    val latestContent by rememberUpdatedState(content)
    val latestSelectedImage by rememberUpdatedState(selectedImage)
    val latestDraftImagePath by rememberUpdatedState(draftImagePath)
    val latestUseWatermark by rememberUpdatedState(useWatermark)
    val latestCurrentDraftId by rememberUpdatedState(currentDraftId)
    val latestSuppressDraftPersistenceOnDispose by rememberUpdatedState(suppressDraftPersistenceOnDispose)
    val latestMode by rememberUpdatedState(mode)

    DisposableEffect(screenKey) {
        onDispose {
            if (latestSuppressDraftPersistenceOnDispose) {
                return@onDispose
            }

            draftPersistenceScope.launch {
                val hasDraftContent = latestName.isNotEmpty() ||
                    latestEmail.isNotEmpty() ||
                    latestTitle.isNotEmpty() ||
                    latestContent.isNotEmpty() ||
                    latestSelectedImage != null ||
                    latestDraftImagePath != null

                runCatching {
                    if (!hasDraftContent) {
                        latestCurrentDraftId?.let { draftId ->
                            repository.deletePostingDraft(draftId)
                        }
                        context.deleteDraftImage(latestDraftImagePath)
                    } else {
                        val currentImage = latestSelectedImage
                        val persistedImagePath = if (currentImage != null) {
                            latestDraftImagePath ?: context.persistDraftImage(
                                image = currentImage,
                                existingPath = null
                            )
                        } else {
                            null
                        }

                        when (val currentMode = latestMode) {
                            is PostComposerMode.Thread -> {
                                repository.saveThreadDraft(
                                    draftId = latestCurrentDraftId,
                                    request = ThreadPostRequest(
                                        forumId = latestSelectedForumId,
                                        name = latestName,
                                        email = latestEmail,
                                        title = latestTitle,
                                        content = latestContent,
                                        useWatermark = latestSelectedImage != null && latestUseWatermark,
                                        image = latestSelectedImage
                                    ),
                                    imagePath = persistedImagePath
                                )
                            }

                            is PostComposerMode.Reply -> {
                                repository.saveReplyDraft(
                                    draftId = latestCurrentDraftId,
                                    request = ReplyPostRequest(
                                        threadId = currentMode.threadId,
                                        name = latestName,
                                        email = latestEmail,
                                        title = latestTitle,
                                        content = latestContent,
                                        useWatermark = latestSelectedImage != null && latestUseWatermark,
                                        image = latestSelectedImage
                                    ),
                                    imagePath = persistedImagePath
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mode is PostComposerMode.Thread && isForumPickerVisible) {
        ForumPickerDialog(
            options = forumOptions,
            selectedForumId = selectedForumId,
            onDismiss = { isForumPickerVisible = false },
            onConfirm = { forumId ->
                mode.onClearError()
                selectedForumId = forumId
                isForumPickerVisible = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        when (mode) {
                            is PostComposerMode.Thread -> {
                                Text("发串")
                                Text(
                                    text = selectedForum?.name ?: "板块 No.${mode.initialForumId ?: TECH_SUPPORT_FORUM_ID}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            is PostComposerMode.Reply -> {
                                Text("回帖")
                                Text(
                                    text = displayReplyTitle ?: "串 No.${mode.threadId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                mode.forumName?.takeIf { it.isNotBlank() }?.let { boardName ->
                                    Text(
                                        text = boardName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    when (mode) {
                        is PostComposerMode.Thread -> {
                            IconButton(onClick = mode.onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        }

                        is PostComposerMode.Reply -> {
                            IconButton(onClick = mode.onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSubmit,
                        onClick = {
                            hasTriedSubmit = true
                            if (!canSubmit) {
                                return@TextButton
                            }
                            scope.launch {
                                mode.onClearError()
                                imageError = null
                                val preparedImage = selectedImage?.let { currentImage ->
                                    isPreparingImage = true
                                    val result = runCatching {
                                        context.preparePostImageForSending(currentImage)
                                    }
                                    isPreparingImage = false
                                    val processedImage = result.getOrElse { throwable ->
                                        imageError = ErrorPresentation(
                                            summary = "处理图片失败",
                                            detail = throwable.message
                                        )
                                        return@launch
                                    }
                                    if (!processedImage.hasSamePayloadAs(currentImage)) {
                                        if (draftImagePath != null) {
                                            context.deleteDraftImage(draftImagePath)
                                            draftImagePath = null
                                        }
                                        draftImageFileName = processedImage.fileName
                                        draftImageMimeType = processedImage.mimeType
                                        selectedImage = processedImage
                                    }
                                    processedImage
                                }

                                suppressDraftPersistenceOnDispose = true
                                when (mode) {
                                    is PostComposerMode.Thread -> {
                                        mode.onSubmit(
                                            ThreadPostRequest(
                                                forumId = selectedForumId,
                                                name = name,
                                                email = email,
                                                title = title,
                                                content = content,
                                                useWatermark = preparedImage != null && useWatermark,
                                                image = preparedImage
                                            ),
                                            currentDraftId
                                        )
                                    }

                                    is PostComposerMode.Reply -> {
                                        mode.onSubmit(
                                            ReplyPostRequest(
                                                threadId = mode.threadId,
                                                name = name,
                                                email = email,
                                                title = title,
                                                content = content,
                                                useWatermark = preparedImage != null && useWatermark,
                                                image = preparedImage
                                            ),
                                            currentDraftId
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (mode.isPosting) "发送中…" else "发送")
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "联调阶段请只在技术支持板块测试。",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (mode) {
                                is PostComposerMode.Thread -> {
                                    "当前实现不会自动补管理员字段，也不会对发串请求做镜像重试，避免误发两次。正文和图片至少要有一个；只有图片时服务端会自动补成“分享图片”。"
                                }

                                is PostComposerMode.Reply -> {
                                    "发送成功后会返回当前串并刷新已加载页数；如果目标楼层不在当前已加载范围内，需要继续翻页查看。正文和图片至少要有一个；只有图片时服务端会自动补成“分享图片”。"
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            activePostCookie?.let { cookie ->
                item {
                    ActivePostCookieCard(
                        cookie = cookie,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } ?: item {
                ErrorMessageCard(
                    error = ErrorPresentation(
                        summary = "未设置发言饼干",
                        detail = "请先在设置里指定一块用于发言的饼干。"
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            mode.error?.let { errorState ->
                item {
                    ErrorMessageCard(
                        error = errorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            draftError?.let { draftErrorState ->
                item {
                    ErrorMessageCard(
                        error = draftErrorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            imageError?.let { imageErrorState ->
                item {
                    ErrorMessageCard(
                        error = imageErrorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (mode is PostComposerMode.Thread) {
                item {
                    OutlinedButton(
                        onClick = { isForumPickerVisible = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("板块")
                            Text(
                                text = selectedForum?.name ?: "板块 No.$selectedForumId",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = selectedForum?.groupName.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PostDraftFields(
                    name = name,
                    onNameChange = {
                        mode.onClearError()
                        name = it
                    },
                    email = email,
                    onEmailChange = {
                        mode.onClearError()
                        email = it
                    },
                    title = title,
                    onTitleChange = {
                        mode.onClearError()
                        title = it
                    },
                    content = content,
                    onContentChange = {
                        mode.onClearError()
                        content = it
                    },
                    isContentInvalid = isContentInvalid,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                PostImageCard(
                    selectedImage = selectedImage,
                    useWatermark = useWatermark,
                    isImageTooLarge = isSelectedImageTooLarge,
                    isPreparingImage = isPreparingImage,
                    onPickImage = { imagePicker.launch("image/*") },
                    onPreviewImage = selectedImage?.let {
                        {
                            scope.launch {
                                val currentImage = selectedImage ?: return@launch
                                val persistedPath = draftImagePath ?: context.persistDraftImage(
                                    image = currentImage,
                                    existingPath = null
                                ).also { draftImagePath = it }
                                draftImageFileName = currentImage.fileName
                                draftImageMimeType = currentImage.mimeType
                                mode.onPreviewImage(persistedPath)
                            }
                        }
                    },
                    onRemoveImage = {
                        mode.onClearError()
                        selectedImage = null
                        imageError = null
                        if (draftImagePath != null) {
                            scope.launch {
                                context.deleteDraftImage(draftImagePath)
                            }
                            draftImagePath = null
                        }
                        draftImageFileName = null
                        draftImageMimeType = null
                    },
                    onUseWatermarkChange = {
                        mode.onClearError()
                        useWatermark = it
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (mode is PostComposerMode.Thread) {
                item {
                    Text(
                        text = "发送成功后会优先尝试打开新串；如果无法拿到串号，则回到对应板块并刷新第一页。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumPickerDialog(
    options: List<PostForumOption>,
    selectedForumId: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择板块") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(option.id) }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (option.id == selectedForumId) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                            color = if (option.id == selectedForumId) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = option.groupName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
