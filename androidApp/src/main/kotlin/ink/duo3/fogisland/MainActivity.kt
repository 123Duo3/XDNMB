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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ink.duo3.fogisland.ui.FogIslandApp
import ink.duo3.fogisland.data.themeSettingsFlow
import ink.duo3.fogisland.data.ThemeSettings
import ink.duo3.fogisland.data.LocalThemeSettings
import ink.duo3.fogisland.ui.theme.FogIslandTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            val themeSettings by applicationContext.themeSettingsFlow.collectAsState(ThemeSettings())

            CompositionLocalProvider(LocalThemeSettings provides themeSettings) {
                FogIslandTheme(themeSettings = themeSettings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        FogIslandApp()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    FogIslandTheme {
        FogIslandApp()
    }
}