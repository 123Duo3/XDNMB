package ink.duo3.xdnmb.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ink.duo3.xdnmb.android.ui.theme.AppTheme
import ink.duo3.xdnmb.android.viewmodel.AppViewModel

@Composable
fun App(
    viewModel: AppViewModel
) {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LaunchedEffect(Unit) {
                viewModel.init()
            }

            val forumList by viewModel.forumList.collectAsState()
            val threadList by viewModel.timeLine.collectAsState()

            ForumsDisplay(forumList, threadList, {
                viewModel.setForumId(it.toInt())
            })
        }
    }
}