package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.util.NmbTimeDisplayMode
import ink.duo3.fogisland.shared.util.NmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.NmbTimeZoneMode

data class ForumTimeSettings(
    val useUtcPlus8Time: Boolean = true,
    val usePreciseTime: Boolean = false,
    val showSeconds: Boolean = false
)

enum class CacheCleanupTtlPolicy(
    val ttlDays: Int?
) {
    NEVER(ttlDays = null),
    ONE_WEEK(ttlDays = 7),
    ONE_MONTH(ttlDays = 30),
    THREE_MONTHS(ttlDays = 92),
    SIX_MONTHS(ttlDays = 183),
    ONE_YEAR(ttlDays = 365);

    fun ttlMillis(): Long? {
        return ttlDays?.toLong()?.times(24L * 60L * 60L * 1000L)
    }
}

fun ForumTimeSettings.toNmbTimeFormatOptions(
    mode: NmbTimeDisplayMode = if (usePreciseTime) {
        NmbTimeDisplayMode.PRECISE
    } else {
        NmbTimeDisplayMode.RELATIVE
    },
    showSeconds: Boolean = this.showSeconds
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
