package ink.duo3.fogisland.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.shared.model.SiteNotice

@Composable
fun SiteNoticeCard(
    notice: SiteNotice,
    publishedAtText: String?,
    onDismissClick: () -> Unit,
    onDismissPermanentlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = MaterialTheme.shapes.large
    Card(
        modifier = modifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    text = "公告",
                    style = MaterialTheme.typography.titleLarge
                )
                publishedAtText?.let { publishedAt ->
                    Text(
                        text = publishedAt,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                text = notice.contentText,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissPermanentlyClick,
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("不再提醒")
                }
                Button(
                    onClick = onDismissClick,
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Preview(name = "Site Notice Card", widthDp = 412)
@Composable
private fun SiteNoticeCardPreview() {
    FogIslandPreviewColumn {
        SiteNoticeCard(
            notice = NmbPreviewSamples.siteNotice,
            publishedAtText = "3 月 29 日",
            onDismissClick = {},
            onDismissPermanentlyClick = {}
        )
    }
}
