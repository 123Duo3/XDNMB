package ink.duo3.xdnmb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ink.duo3.xdnmb.android.ui.App
import ink.duo3.xdnmb.android.viewmodel.AppViewModel
import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.cache.DatabaseDriverFactory

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<AppViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return modelClass.getConstructor(XdSDK::class.java).newInstance(
                    XdSDK(DatabaseDriverFactory(applicationContext))
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false);
        setContent {
            App(viewModel = viewModel)
        }
    }
}
