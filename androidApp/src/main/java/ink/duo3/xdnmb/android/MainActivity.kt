package ink.duo3.xdnmb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import ink.duo3.xdnmb.XdSDK
import ink.duo3.xdnmb.android.ui.ForumsDisplay
import ink.duo3.xdnmb.android.ui.theme.AppTheme
import ink.duo3.xdnmb.data.cache.DatabaseDriverFactory
import ink.duo3.xdnmb.data.model.ForumGroup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ForumsDisplay(forumList = getForums())
                }
            }
        }
    }
    private val sdk = XdSDK(DatabaseDriverFactory(this))

    fun getForums(): List<ForumGroup>? {
        var forumList: List<ForumGroup>? = null
        suspend {
            forumList = sdk.getForumList(true)
        }
        return forumList
    }
}
