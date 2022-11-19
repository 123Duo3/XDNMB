package ink.duo3.xdnmb.android.ui.component

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import ink.duo3.xdnmb.android.ui.theme.AppTheme

@Composable
fun HtmlText(
    html: String,
    style: TextStyle = LocalTextStyle.current,
    clickable: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    val textColor = LocalContentColor.current
    HtmlComposeText(html, style.copy(color = textColor), clickable, maxLines)
}


@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HtmlText(
                html = """
                    Hello, <span style="color: green"><i>world</i></span><a href="https://www.example.com">test</a>
                """.trimIndent()
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun HtmlComposeText(
    html: String,
    style: TextStyle,
    clickable: Boolean,
    maxLines: Int
) {
    val annotatedText = buildAnnotatedStringFromHtml(html, style)
    val context = LocalContext.current

    if(clickable) {
        ClickableText(
            text = annotatedText,
            style = style,
            onClick = {
                annotatedText.getUrlAnnotations(it, it)
                    .firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item.url)))
                    }
            },
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = annotatedText,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun buildAnnotatedStringFromHtml(html: String, style: TextStyle): AnnotatedString {
    val text = remember(html) { HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    val urlColor = MaterialTheme.colorScheme.primary
    return remember(html) {
        text.toSpannable().toAnnotatedString(style, urlColor)
    }
}


fun Spannable.toAnnotatedString(style: TextStyle, accentColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder(this.toString())
    builder.addStyle(style.toSpanStyle(), 0, builder.length)
    val copierContext = CopierContext(style.color, accentColor)
    SpanCopier.values().forEach { copier ->
        getSpans(0, length, copier.spanClass).forEach { span ->
            copier.copySpan(span, getSpanStart(span), getSpanEnd(span), builder, copierContext)
        }
    }
    return builder.toAnnotatedString()
}

private data class CopierContext(
    val normalColor: Color,
    val accentColor: Color
)

private enum class SpanCopier {
    URL {
        override val spanClass = URLSpan::class.java

        @OptIn(ExperimentalTextApi::class)
        override fun copySpan(
            span: Any,
            start: Int,
            end: Int,
            destination: AnnotatedString.Builder,
            context: CopierContext
        ) {
            val urlSpan = span as URLSpan
            destination.addUrlAnnotation(UrlAnnotation(urlSpan.url), start, end)
            destination.addStyle(
                style = SpanStyle(
                    color = context.accentColor,
                    textDecoration = TextDecoration.Underline
                ),
                start = start,
                end = end,
            )
        }
    },
    FOREGROUND_COLOR {
        override val spanClass = ForegroundColorSpan::class.java
        override fun copySpan(
            span: Any,
            start: Int,
            end: Int,
            destination: AnnotatedString.Builder,
            context: CopierContext
        ) {
            val colorSpan = span as ForegroundColorSpan
            destination.addStyle(
                style = SpanStyle(color = Color(colorSpan.foregroundColor)),
                start = start,
                end = end,
            )
        }
    },
    UNDERLINE {
        override val spanClass = UnderlineSpan::class.java
        override fun copySpan(
            span: Any,
            start: Int,
            end: Int,
            destination: AnnotatedString.Builder,
            context: CopierContext
        ) {
            destination.addStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline),
                start = start,
                end = end,
            )
        }
    },
    STYLE {
        override val spanClass = StyleSpan::class.java
        override fun copySpan(
            span: Any,
            start: Int,
            end: Int,
            destination: AnnotatedString.Builder,
            context: CopierContext
        ) {
            val styleSpan = span as StyleSpan

            destination.addStyle(
                style = when (styleSpan.style) {
                    Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    Typeface.BOLD_ITALIC -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )

                    else -> SpanStyle()
                },
                start = start,
                end = end,
            )
        }
    };

    abstract val spanClass: Class<out CharacterStyle>
    abstract fun copySpan(
        span: Any,
        start: Int,
        end: Int,
        destination: AnnotatedString.Builder,
        context: CopierContext
    )
}