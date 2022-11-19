package ink.duo3.xdnmb.android.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ink.duo3.xdnmb.shared.data.entity.Thread

//@Composable
//fun ReplyHead(thread: Thread) {
//
//}

@Composable
fun ReplyHead(thread: Thread) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = thread.userHash,
            color = if (true) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(text = " • ")
        Text(text = thread.time)
    }

}

@Composable
fun ReplyItem(thread: Thread) {

}

@Preview
@Composable
fun ReplyItemPreview(){
    val reply = Thread(id=51888946, fid=4, replyCount=331, img="2022-09-10/631b704f9e7e5", ext=".jpg", time="2022-09-10(六)00:56:45", userHash="l3dkaJL", name="无名氏", title="无标题", content="是我笑点太低了吗( ﾟ∀。)<br />我根本忍不住看一次笑一次( ﾟ∀。)", sage=0, admin=0, hide=0, replies=null, remainReplies=326, email=null, poster=null, page=1, forumName="")
    ReplyHead(thread = reply)
}
