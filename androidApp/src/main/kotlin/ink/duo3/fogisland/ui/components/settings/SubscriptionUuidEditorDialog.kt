package ink.duo3.fogisland.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.fillMaxWidth
import ink.duo3.fogisland.data.updateSubscriptionUuid
import ink.duo3.fogisland.shared.storage.preferences.MAX_SUBSCRIPTION_UUID_LENGTH
import ink.duo3.fogisland.shared.storage.preferences.generateSubscriptionUuid
import ink.duo3.fogisland.shared.storage.preferences.isSubscriptionUuidFormatValid
import ink.duo3.fogisland.shared.storage.preferences.normalizeSubscriptionUuidInput
import kotlinx.coroutines.launch

@Composable
fun SubscriptionUuidEditorDialog(
    currentUuid: String?,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable(currentUuid, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(subscriptionUuidTextFieldValue(currentUuid.orEmpty()))
    }
    var saveErrorMessage by rememberSaveable(currentUuid) { mutableStateOf<String?>(null) }
    val validationErrorMessage = validateSubscriptionUuidText(draft.text)
    val supportingMessage = saveErrorMessage ?: validationErrorMessage

    SubscriptionUuidEditorDialogContent(
        draft = draft,
        errorMessage = supportingMessage,
        onDraftChange = {
            draft = normalizeSubscriptionUuidFieldValue(it)
            saveErrorMessage = null
        },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val normalizedValue = normalizeSubscriptionUuidInput(draft.text)
            if (validationErrorMessage != null) {
                return@SubscriptionUuidEditorDialogContent
            }
            scope.launch {
                runCatching {
                    context.updateSubscriptionUuid(normalizedValue)
                }.onSuccess {
                    saveErrorMessage = null
                    onDismissRequest()
                    onSaved()
                }.onFailure { throwable ->
                    saveErrorMessage = throwable.message
                        ?.ifBlank { "保存订阅 ID 失败" }
                        ?: "保存订阅 ID 失败"
                }
            }
        },
        isInputError = validationErrorMessage != null
    )
}

@Composable
private fun SubscriptionUuidEditorDialogContent(
    draft: TextFieldValue,
    errorMessage: String?,
    onDraftChange: (TextFieldValue) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    isInputError: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("设置订阅 ID") },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("订阅 ID") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onDraftChange(subscriptionUuidTextFieldValue(generateSubscriptionUuid()))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "随机生成订阅 ID"
                        )
                    }
                },
                isError = isInputError,
                supportingText = {
                    errorMessage?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    } ?: Text("修改后会清空当前订阅缓存，并切换到对应 ID 的订阅列表。")
                },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = draft.text.isNotBlank() && !isInputError
            ) {
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

private fun validateSubscriptionUuidText(text: String): String? {
    val normalizedText = normalizeSubscriptionUuidInput(text)
    return when {
        normalizedText.isBlank() -> null
        normalizedText.length > MAX_SUBSCRIPTION_UUID_LENGTH ->
            "订阅 ID 最长 $MAX_SUBSCRIPTION_UUID_LENGTH 个字符"
        isSubscriptionUuidFormatValid(normalizedText) -> null
        else -> "订阅 ID 格式无效"
    }
}

private fun subscriptionUuidTextFieldValue(text: String): TextFieldValue {
    val normalizedText = normalizeSubscriptionUuidInput(text)
    return TextFieldValue(
        text = normalizedText,
        selection = TextRange(normalizedText.length)
    )
}

private fun normalizeSubscriptionUuidFieldValue(value: TextFieldValue): TextFieldValue {
    val rawText = value.text
    val normalizedText = normalizeSubscriptionUuidInput(rawText)
    val trimmedStartLength = rawText.length - rawText.trimStart().length
    val normalizedSelectionStart = (value.selection.start - trimmedStartLength)
        .coerceIn(0, normalizedText.length)
    val normalizedSelectionEnd = (value.selection.end - trimmedStartLength)
        .coerceIn(0, normalizedText.length)
    return TextFieldValue(
        text = normalizedText,
        selection = TextRange(normalizedSelectionStart, normalizedSelectionEnd)
    )
}
