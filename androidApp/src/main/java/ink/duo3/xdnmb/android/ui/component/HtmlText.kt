package ink.duo3.xdnmb.android.ui.component

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import ink.duo3.xdnmb.android.ui.theme.AppTheme

@Composable
fun HtmlText(
    html: String
) {
    AndroidView(
        factory = {
            TextView(it)
        }
    ) {
        it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}


@Preview
@Composable
private fun Preview() {
    AppTheme {
        HtmlText(html = """
            <span style="color: red">Hello, <i>world</i></span>
        """.trimIndent())
    }
}