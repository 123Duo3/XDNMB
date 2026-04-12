package ink.duo3.fogisland.ui.components.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.shared.model.ResolvedPostReference

internal sealed interface ReferenceDialogState {
    data class Loading(val postId: Long) : ReferenceDialogState
    data class Resolved(val reference: ResolvedPostReference) : ReferenceDialogState
    data class Error(val postId: Long, val message: String) : ReferenceDialogState
}

@Composable
internal fun ReferencePostDialog(
    state: ReferenceDialogState,
    onDismiss: () -> Unit,
    onOpenThread: (ResolvedPostReference) -> Unit,
    onImageClick: (String, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (state) {
                    is ReferenceDialogState.Loading -> ">>No.${state.postId}"
                    is ReferenceDialogState.Resolved -> ">>No.${state.reference.post.remoteId}"
                    is ReferenceDialogState.Error -> ">>No.${state.postId}"
                }
            )
        },
        text = {
            when (state) {
                is ReferenceDialogState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ReferenceDialogState.Resolved -> {
                    NmbPostFlatItem(
                        post = state.reference.post,
                        onImageClick = onImageClick,
                        onLinkClick = null,
                        showThreadId = false,
                        showDivider = false,
                        showPosterLabel = !state.reference.post.isThread,
                        contentPadding = PaddingValues(0.dp),
                        bodyMaxLines = 12,
                        bodyOverflow = TextOverflow.Ellipsis
                    )
                }

                is ReferenceDialogState.Error -> Text(state.message)
            }
        },
        confirmButton = {
            if (state is ReferenceDialogState.Resolved) {
                TextButton(onClick = { onOpenThread(state.reference) }) {
                    Text("跳转到原串")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
