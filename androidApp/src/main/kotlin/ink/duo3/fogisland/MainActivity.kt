package ink.duo3.fogisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.ui.FogIslandApp
import ink.duo3.fogisland.data.LocalThemeSettings
import ink.duo3.fogisland.data.ThemeSettings
import ink.duo3.fogisland.data.themeSettingsFlow
import ink.duo3.fogisland.data.timeSettingsFlow
import ink.duo3.fogisland.shared.model.ForumTimeSettings
import ink.duo3.fogisland.ui.theme.FogIslandTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val incomingIntents = MutableSharedFlow<android.content.Intent>(replay = 1, extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        incomingIntents.tryEmit(intent)
        
        setContent {
            val themeSettings by applicationContext.themeSettingsFlow.collectAsState(ThemeSettings())
            val timeSettings by applicationContext.timeSettingsFlow.collectAsState(ForumTimeSettings())

            CompositionLocalProvider(
                LocalThemeSettings provides themeSettings,
                LocalTimeSettings provides timeSettings
            ) {
                FogIslandTheme(themeSettings = themeSettings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        FogIslandApp(incomingIntents = incomingIntents)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntents.tryEmit(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    FogIslandTheme {
        FogIslandApp()
    }
}
