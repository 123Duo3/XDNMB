package ink.duo3.fogisland

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fog Island",
    ) {
        App()
    }
}