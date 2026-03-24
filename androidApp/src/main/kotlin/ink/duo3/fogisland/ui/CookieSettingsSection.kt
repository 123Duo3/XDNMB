package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.cookieCollectionFlow
import ink.duo3.fogisland.data.deleteCookie
import ink.duo3.fogisland.data.importCookie
import ink.duo3.fogisland.data.moveCookie
import ink.duo3.fogisland.data.updateActivePostCookie
import ink.duo3.fogisland.data.updateActiveRequestCookie
import ink.duo3.fogisland.data.updateCookieRemark
import ink.duo3.fogisland.shared.model.CookieCollection
import ink.duo3.fogisland.shared.model.CookieProfile
import ink.duo3.fogisland.shared.model.MAX_COOKIE_PROFILE_COUNT
import ink.duo3.fogisland.shared.storage.preferences.CookieLimitExceededException
import ink.duo3.fogisland.ui.components.SettingItem
import ink.duo3.fogisland.ui.components.SettingItemGroup
import kotlinx.coroutines.launch

@Composable
fun CookieSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cookieCollection by context.cookieCollectionFlow.collectAsState(
        CookieCollection(
            cookies = emptyList(),
            activeRequestCookieId = null,
            activePostCookieId = null
        )
    )

    var isImportDialogVisible by rememberSaveable { mutableStateOf(false) }
    var importPayload by rememberSaveable { mutableStateOf("") }
    var importRemark by rememberSaveable { mutableStateOf("") }
    var importErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCookieId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingRemark by rememberSaveable { mutableStateOf("") }
    var deletingCookieId by rememberSaveable { mutableStateOf<String?>(null) }
    var sectionMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val editingCookie = cookieCollection.cookies.firstOrNull { it.id == editingCookieId }
    val deletingCookie = cookieCollection.cookies.firstOrNull { it.id == deletingCookieId }

    LaunchedEffect(editingCookieId, editingCookie) {
        if (editingCookieId != null && editingCookie == null) {
            editingCookieId = null
            editingRemark = ""
        }
    }

    LaunchedEffect(deletingCookieId, deletingCookie) {
        if (deletingCookieId != null && deletingCookie == null) {
            deletingCookieId = null
        }
    }

    SettingItemGroup(
        title = "饼干",
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("最多保存 $MAX_COOKIE_PROFILE_COUNT 块饼干，请求和发言可以分别指定。")
                sectionMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) {
        SettingItem(
            title = { Text("导入饼干") },
            description = {
                Text("粘贴二维码扫描结果或 userhash 字符串。已保存 ${cookieCollection.cookies.size}/$MAX_COOKIE_PROFILE_COUNT。")
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = {
                importPayload = ""
                importRemark = ""
                importErrorMessage = null
                isImportDialogVisible = true
            }
        )

        if (cookieCollection.cookies.isEmpty()) {
            SettingItem(
                title = { Text("尚未导入饼干") },
                description = { Text("导入后可以分别设置请求头鉴权和发言使用的默认饼干。") },
                icon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
            )
        } else {
            cookieCollection.cookies.forEachIndexed { index, cookie ->
                CookieProfileItem(
                    cookie = cookie,
                    index = index,
                    cookieCount = cookieCollection.cookies.size,
                    isActiveRequestCookie = cookie.id == cookieCollection.activeRequestCookieId,
                    isActivePostCookie = cookie.id == cookieCollection.activePostCookieId,
                    onSetRequestCookie = {
                        scope.launch {
                            runCatching {
                                context.updateActiveRequestCookie(cookie.id)
                            }.onSuccess {
                                sectionMessage = null
                            }.onFailure { throwable ->
                                sectionMessage = throwable.toCookieMessage()
                            }
                        }
                    },
                    onSetPostCookie = {
                        scope.launch {
                            runCatching {
                                context.updateActivePostCookie(cookie.id)
                            }.onSuccess {
                                sectionMessage = null
                            }.onFailure { throwable ->
                                sectionMessage = throwable.toCookieMessage()
                            }
                        }
                    },
                    onEditRemark = {
                        editingCookieId = cookie.id
                        editingRemark = cookie.remark
                    },
                    onMoveUp = if (index > 0) {
                        {
                            scope.launch {
                                runCatching {
                                    context.moveCookie(cookie.id, index - 1)
                                }.onSuccess {
                                    sectionMessage = null
                                }.onFailure { throwable ->
                                    sectionMessage = throwable.toCookieMessage()
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onMoveDown = if (index < cookieCollection.cookies.lastIndex) {
                        {
                            scope.launch {
                                runCatching {
                                    context.moveCookie(cookie.id, index + 1)
                                }.onSuccess {
                                    sectionMessage = null
                                }.onFailure { throwable ->
                                    sectionMessage = throwable.toCookieMessage()
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onDelete = {
                        deletingCookieId = cookie.id
                    }
                )
            }
        }
    }

    if (isImportDialogVisible) {
        CookieImportDialog(
            payload = importPayload,
            onPayloadChange = {
                importPayload = it
                importErrorMessage = null
            },
            remark = importRemark,
            onRemarkChange = { importRemark = it },
            errorMessage = importErrorMessage,
            onDismissRequest = {
                isImportDialogVisible = false
                importErrorMessage = null
            },
            onConfirm = {
                scope.launch {
                    runCatching {
                        context.importCookie(
                            rawPayload = importPayload,
                            remark = importRemark.takeIf { it.isNotBlank() }
                        )
                    }.onSuccess {
                        isImportDialogVisible = false
                        importPayload = ""
                        importRemark = ""
                        importErrorMessage = null
                        sectionMessage = null
                    }.onFailure { throwable ->
                        importErrorMessage = throwable.toCookieMessage()
                    }
                }
            }
        )
    }

    if (editingCookie != null) {
        CookieRemarkDialog(
            currentCookie = editingCookie,
            remark = editingRemark,
            onRemarkChange = { editingRemark = it },
            onDismissRequest = {
                editingCookieId = null
                editingRemark = ""
            },
            onConfirm = {
                scope.launch {
                    runCatching {
                        context.updateCookieRemark(editingCookie.id, editingRemark)
                    }.onSuccess {
                        editingCookieId = null
                        editingRemark = ""
                        sectionMessage = null
                    }.onFailure { throwable ->
                        sectionMessage = throwable.toCookieMessage()
                    }
                }
            }
        )
    }

    if (deletingCookie != null) {
        DeleteCookieDialog(
            cookie = deletingCookie,
            onDismissRequest = { deletingCookieId = null },
            onConfirm = {
                scope.launch {
                    runCatching {
                        context.deleteCookie(deletingCookie.id)
                    }.onSuccess {
                        deletingCookieId = null
                        sectionMessage = null
                    }.onFailure { throwable ->
                        sectionMessage = throwable.toCookieMessage()
                    }
                }
            }
        )
    }
}

@Composable
private fun CookieProfileItem(
    cookie: CookieProfile,
    index: Int,
    cookieCount: Int,
    isActiveRequestCookie: Boolean,
    isActivePostCookie: Boolean,
    onSetRequestCookie: () -> Unit,
    onSetPostCookie: () -> Unit,
    onEditRemark: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit
) {
    SettingItem(
        title = {
            Text(
                text = cookie.resolveTitle(index),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        description = {
            Text(
                text = cookie.buildDescription(
                    isActiveRequestCookie = isActiveRequestCookie,
                    isActivePostCookie = isActivePostCookie
                )
            )
        },
        icon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
        bottomAction = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onSetRequestCookie,
                    enabled = !isActiveRequestCookie
                ) {
                    Text(if (isActiveRequestCookie) "正在请求" else "设为请求")
                }
                TextButton(
                    onClick = onSetPostCookie,
                    enabled = !isActivePostCookie
                ) {
                    Text(if (isActivePostCookie) "正在发言" else "设为发言")
                }
                TextButton(onClick = onEditRemark) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(
                        text = if (cookie.remark.isBlank()) "备注" else "改备注",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                TextButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                    Text("上移", modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null && index < cookieCount - 1
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                    Text("下移", modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("删除", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    )
}

@Composable
private fun CookieImportDialog(
    payload: String,
    onPayloadChange: (String) -> Unit,
    remark: String,
    onRemarkChange: (String) -> Unit,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("导入饼干") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = payload,
                    onValueChange = onPayloadChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("二维码内容或 userhash") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    singleLine = true
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = payload.isNotBlank()
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CookieRemarkDialog(
    currentCookie: CookieProfile,
    remark: String,
    onRemarkChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("备注饼干") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(currentCookie.resolveTitle(index = 0))
                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DeleteCookieDialog(
    cookie: CookieProfile,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("删除饼干") },
        text = {
            Text("确定删除 ${cookie.displayName ?: "这块饼干"} 吗？当前与它关联的请求/发言默认设置也会被清空。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

private fun CookieProfile.resolveTitle(index: Int): String {
    return displayName ?: "饼干 ${index + 1}"
}

private fun CookieProfile.buildDescription(
    isActiveRequestCookie: Boolean,
    isActivePostCookie: Boolean
): String {
    val lines = buildList {
        if (remark.isNotBlank()) {
            add("备注 $remark")
        }
        add("标识 ${maskCookieValue(cookieValue)}")
        add(
            when {
                isActiveRequestCookie && isActivePostCookie -> "当前用于请求和发言"
                isActiveRequestCookie -> "当前用于请求"
                isActivePostCookie -> "当前用于发言"
                else -> "未设为默认饼干"
            }
        )
    }
    return lines.joinToString("\n")
}

private fun maskCookieValue(rawValue: String): String {
    val normalizedValue = rawValue.trim()
    val cookieValue = normalizedValue.substringAfter('=', normalizedValue)
    if (cookieValue.length <= 8) {
        return cookieValue
    }
    return "${cookieValue.take(4)}…${cookieValue.takeLast(4)}"
}

private fun Throwable.toCookieMessage(): String {
    return when (this) {
        is CookieLimitExceededException -> message.orEmpty()
        is IllegalArgumentException -> message.orEmpty().ifBlank { "饼干内容无效" }
        else -> message.orEmpty().ifBlank { "操作失败" }
    }
}
