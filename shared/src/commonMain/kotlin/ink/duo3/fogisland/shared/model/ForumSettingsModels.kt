package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.util.NmbTimeDisplayMode
import ink.duo3.fogisland.shared.util.NmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.NmbTimeZoneMode

data class ForumTimeSettings(
    val useUtcPlus8Time: Boolean = true
)

fun ForumTimeSettings.toNmbTimeFormatOptions(
    mode: NmbTimeDisplayMode = NmbTimeDisplayMode.RELATIVE,
    showSeconds: Boolean = false
): NmbTimeFormatOptions {
    return NmbTimeFormatOptions(
        mode = mode,
        showSeconds = showSeconds,
        timeZoneMode = if (useUtcPlus8Time) {
            NmbTimeZoneMode.UTC_PLUS_8
        } else {
            NmbTimeZoneMode.LOCAL
        }
    )
}
