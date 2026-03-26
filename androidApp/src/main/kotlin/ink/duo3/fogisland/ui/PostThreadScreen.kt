package ink.duo3.fogisland.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.cookieCollectionFlow
import ink.duo3.fogisland.data.draft.deleteDraftImage
import ink.duo3.fogisland.data.draft.persistDraftImage
import ink.duo3.fogisland.data.draft.readDraftImage
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.PostingDraftEntry
import ink.duo3.fogisland.shared.model.ThreadPostImage
import ink.duo3.fogisland.shared.model.ThreadPostRequest
import ink.duo3.fogisland.shared.network.api.toErrorPresentation
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.ui.components.ActivePostCookieCard
import ink.duo3.fogisland.ui.components.compressPostImage
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.isPostImageTooLarge
import ink.duo3.fogisland.ui.components.PostDraftFields
import ink.duo3.fogisland.ui.components.PostImageCard
import ink.duo3.fogisland.ui.components.readPostImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

private const val TECH_SUPPORT_FORUM_ID = 117L
private const val TECH_SUPPORT_FORUM_NAME = "技术支持"

private data class PostForumOption(
    val id: Long,
    val name: String,
    val displayName: String,
    val groupName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostThreadScreen(
    forumGroups: List<ForumGroup>,
    initialForumId: Long?,
    initialDraft: PostingDraftEntry?,
    isPosting: Boolean,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onClearError: () -> Unit,
    onSubmit: (ThreadPostRequest, Long?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draftPersistenceScope = remember(context.applicationContext) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    val repository = remember(context) { RepositoryProvider.provideForumRepository(context.applicationContext) }
    val cookieCollection by context.cookieCollectionFlow.collectAsState(
        initial = ink.duo3.fogisland.shared.model.CookieCollection(
            cookies = emptyList(),
            activeRequestCookieId = null,
            activePostCookieId = null
        )
    )
    val activePostCookie = remember(cookieCollection) {
        cookieCollection.cookies.firstOrNull { it.id == cookieCollection.activePostCookieId }
    }
    val forumOptions = remember(forumGroups) {
        forumGroups.flatMap { group ->
            group.forums.map { forum ->
                PostForumOption(
                    id = forum.id,
                    name = forum.name.ifBlank { forum.displayName },
                    displayName = forum.displayName,
                    groupName = group.name
                )
            }
        }
    }
    val fallbackForumId = initialForumId ?: TECH_SUPPORT_FORUM_ID
    val initialSelectedForumId = remember(initialForumId, initialDraft, forumOptions) {
        initialDraft?.forumId
            ?.takeIf { forumId -> forumOptions.any { it.id == forumId } }
            ?: initialForumId
            ?.takeIf { forumId -> forumOptions.any { it.id == forumId } }
            ?: forumOptions.firstOrNull { it.id == TECH_SUPPORT_FORUM_ID }?.id
            ?: initialForumId
            ?: TECH_SUPPORT_FORUM_ID
    }
    var selectedForumId by rememberSaveable(initialSelectedForumId) { mutableStateOf(initialSelectedForumId) }
    var name by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.name.orEmpty()) }
    var email by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.email.orEmpty()) }
    var title by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.title.orEmpty()) }
    var content by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.contentText.orEmpty()) }
    var selectedImage by remember(initialDraft?.id) { mutableStateOf<ThreadPostImage?>(null) }
    var draftImagePath by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.imagePath) }
    var useWatermark by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.useWatermark ?: true) }
    var currentDraftId by rememberSaveable(initialDraft?.id) { mutableStateOf(initialDraft?.id) }
    var isForumPickerVisible by rememberSaveable { mutableStateOf(false) }
    var hasTriedSubmit by rememberSaveable { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<ErrorPresentation?>(null) }
    var draftError by remember { mutableStateOf<ErrorPresentation?>(null) }
    var isCompressingImage by remember { mutableStateOf(false) }
    var suppressDraftPersistenceOnDispose by remember { mutableStateOf(false) }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val selectedForum = remember(selectedForumId, forumOptions) {
        forumOptions.firstOrNull { it.id == selectedForumId }
    }
    val hasContentOrImage = content.isNotBlank() || selectedImage != null
    val isContentInvalid = hasTriedSubmit && !hasContentOrImage
    val canSubmit = !isPosting && hasContentOrImage && activePostCookie != null
    val isSelectedImageTooLarge = remember(selectedImage) { isPostImageTooLarge(selectedImage) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            imageError = null
            runCatching {
                context.readPostImage(uri)
            }.onSuccess { image ->
                onClearError()
                if (draftImagePath != null) {
                    context.deleteDraftImage(draftImagePath)
                    draftImagePath = null
                }
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
        if (selectedForum == null) {
            selectedForumId = initialSelectedForumId
        }
    }

    LaunchedEffect(initialDraft?.id) {
        selectedImage = initialDraft?.imagePath?.let { imagePath ->
            context.readDraftImage(
                path = imagePath,
                fileName = initialDraft.imageFileName,
                mimeType = initialDraft.imageMimeType
            )
        }
        if (initialDraft?.imagePath != null && selectedImage == null) {
            draftImagePath = null
        }
    }

    LaunchedEffect(isPosting, error) {
        if (!isPosting && error != null) {
            suppressDraftPersistenceOnDispose = false
        }
    }

    LaunchedEffect(
        selectedForumId,
        name,
        email,
        title,
        content,
        selectedImage,
        useWatermark,
        currentDraftId,
        isPosting
    ) {
        if (isPosting) {
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

            currentDraftId = repository.saveThreadDraft(
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

    DisposableEffect(Unit) {
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
                }
            }
        }
    }

    if (isForumPickerVisible) {
        ForumPickerDialog(
            options = forumOptions,
            selectedForumId = selectedForumId,
            onDismiss = { isForumPickerVisible = false },
            onConfirm = { forumId ->
                onClearError()
                selectedForumId = forumId
                isForumPickerVisible = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .clip(RoundedCornerShape(28.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("发串")
                        Text(
                            text = selectedForum?.name ?: "板块 No.$fallbackForumId",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
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

                            suppressDraftPersistenceOnDispose = true
                            onSubmit(
                                ThreadPostRequest(
                                    forumId = selectedForumId,
                                    name = name,
                                    email = email,
                                    title = title,
                                    content = content,
                                    useWatermark = selectedImage != null && useWatermark,
                                    image = selectedImage
                                ),
                                currentDraftId
                            )
                        }
                    ) {
                        Text(if (isPosting) "发送中…" else "发送")
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
                            text = "当前实现不会自动补管理员字段，也不会对发串请求做镜像重试，避免误发两次。正文和图片至少要有一个；只有图片时服务端会自动补成“分享图片”。",
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

            error?.let { errorState ->
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
                            text = selectedForum?.groupName ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PostDraftFields(
                    name = name,
                    onNameChange = {
                        onClearError()
                        name = it
                    },
                    email = email,
                    onEmailChange = {
                        onClearError()
                        email = it
                    },
                    title = title,
                    onTitleChange = {
                        onClearError()
                        title = it
                    },
                    content = content,
                    onContentChange = {
                        onClearError()
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
                    isCompressingImage = isCompressingImage,
                    onPickImage = { imagePicker.launch("image/*") },
                    onRemoveImage = {
                        onClearError()
                        selectedImage = null
                        imageError = null
                        if (draftImagePath != null) {
                            scope.launch {
                                context.deleteDraftImage(draftImagePath)
                            }
                            draftImagePath = null
                        }
                    },
                    onCompressImage = {
                        selectedImage?.let { image ->
                            scope.launch {
                                onClearError()
                                imageError = null
                                isCompressingImage = true
                                runCatching {
                                    context.compressPostImage(image)
                                }.onSuccess { compressedImage ->
                                    if (draftImagePath != null) {
                                        context.deleteDraftImage(draftImagePath)
                                        draftImagePath = null
                                    }
                                    selectedImage = compressedImage
                                }.onFailure { throwable ->
                                    imageError = ErrorPresentation(
                                        summary = "压缩图片失败",
                                        detail = throwable.message
                                    )
                                }
                                isCompressingImage = false
                            }
                        }
                    },
                    onUseWatermarkChange = {
                        onClearError()
                        useWatermark = it
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

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
