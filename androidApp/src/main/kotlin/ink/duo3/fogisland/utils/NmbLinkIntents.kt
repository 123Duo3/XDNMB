package ink.duo3.fogisland.utils

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.resolveNmbUrlLinkTarget

fun resolveNmbIntentLinkTarget(intent: Intent?): NmbLinkTarget? {
    if (intent?.action != Intent.ACTION_VIEW) {
        return null
    }

    val dataString = intent.dataString ?: return null
    return resolveNmbUrlLinkTarget(dataString)
}

fun openNmbExternalLink(context: Context, url: String) {
    val uri = url.toUri()
    val viewIntent = Intent(Intent.ACTION_VIEW, uri)

    if (uri.scheme == "http" || uri.scheme == "https") {
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        }.onSuccess {
            return
        }
    }

    context.startActivity(viewIntent)
}
