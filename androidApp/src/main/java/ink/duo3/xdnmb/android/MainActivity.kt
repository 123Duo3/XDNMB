package ink.duo3.xdnmb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ink.duo3.xdnmb.android.ui.ForumsDisplay
import ink.duo3.xdnmb.android.ui.theme.AppTheme
import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.cache.DatabaseDriverFactory
import ink.duo3.xdnmb.shared.data.entity.ForumGroup

class MainActivity : ComponentActivity() {
    private var forumList by mutableStateOf<List<ForumGroup>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) {
                init()
            }

            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ForumsDisplay(forumList = forumList)
                }
            }
        }
    }

    private val sdk = XdSDK(DatabaseDriverFactory(this))

    private suspend fun init() {
        forumList = sdk.getForumList(true)
    }
}
