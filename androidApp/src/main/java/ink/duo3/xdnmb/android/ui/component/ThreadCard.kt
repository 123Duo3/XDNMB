package ink.duo3.xdnmb.android.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ink.duo3.xdnmb.android.R
import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.entity.Thread

@Composable
fun ThreadCard(thread: Thread, sdk: XdSDK) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(16.dp, 12.dp)
        ) {
            Row(
                Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = thread.userHash,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (thread.admin == 1) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = sdk.formatTime(thread.time, false),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(Modifier) {
                val hasTitle = thread.title != "无标题"
                val hasName = thread.name != "无名氏"

                if (hasTitle || hasName) {
                    if (hasTitle) Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (hasName) Text(
                        text = thread.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))
                }

                SelectionContainer {
                    HtmlText(
                        html = thread.content,
                        style = MaterialTheme.typography.bodyMedium,
                        clickable = true
                    )
                }
            }

            if (thread.img.isNotBlank()) {
                val context = LocalContext.current
                AsyncImage(
                    model = remember {
                        ImageRequest.Builder(context)
                            .data(sdk.imgToUrl(thread.img, thread.ext, true))
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(128.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Row(
                Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "No." + thread.id,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = sdk.getForumName(thread.fid),
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    painter = painterResource(id = R.drawable.comment_black_24dp),
                    "",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterVertically)
                )
                Text(
                    text = thread.replyCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 6.dp)
                )
            }
        }
    }
}

//@Preview
//@Composable
//fun ThreadCardPreview () {
//    ThreadCard()
//}