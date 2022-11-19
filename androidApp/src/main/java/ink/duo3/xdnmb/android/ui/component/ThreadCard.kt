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
import androidx.compose.runtime.Stable
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
import ink.duo3.xdnmb.android.viewmodel.formatTime
import ink.duo3.xdnmb.shared.data.entity.Thread

@Composable
fun ThreadCard(
    thread: Thread,
    forumId: Int
) {
    ThreadCard(
        thread.name.takeIf { it != "无名氏" },
        thread.userHash,
        thread.admin == 1,
        formatTime(thread.time, false),
        thread.id,
        thread.content,
        thread.title.takeIf { it != "无标题" },
        thread.img.takeIf { it.isNotBlank() }?.let { imgToUrl(it, thread.ext, true) },
        thread.replyCount!!.toInt(),
        thread.forumName?: "",
        forumId
    )
}

@Composable
fun ThreadCard(
    userName: String?,
    userHash: String,
    isAdmin: Boolean,
    time: String,
    threadId: Int,
    content: String,
    title: String?,
    image: String?,
    replyCount: Int,
    forumName: String,
    forumId: Int
) {
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
                    text = userHash,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAdmin) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(Modifier) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                userName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (title != null || userName != null)
                    Spacer(Modifier.height(8.dp))

                SelectionContainer {
                    HtmlText(
                        html = content,
                        style = MaterialTheme.typography.bodyMedium,
                        clickable = false,
                        maxLines = 12
                    )
                }
            }

            image?.let {
                val context = LocalContext.current
                AsyncImage(
                    model = remember {
                        ImageRequest.Builder(context)
                            .data(it)
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
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No.$threadId",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (forumId == -1) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            text = forumName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(id = R.drawable.comment_black_24dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = replyCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Stable
fun imgToUrl(img: String, ext: String, isThumb: Boolean): String {
    var imageType = "image/"
    if (isThumb) {
        imageType = "thumb/"
    }
    return "https://image.nmb.best/$imageType$img$ext"
}

//@Preview
//@Composable
//fun ThreadCardPreview () {
//    ThreadCard()
//}