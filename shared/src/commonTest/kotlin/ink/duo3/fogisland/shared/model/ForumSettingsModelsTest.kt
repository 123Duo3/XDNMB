package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.util.NmbTimeDisplayMode
import ink.duo3.fogisland.shared.util.NmbTimeZoneMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ForumSettingsModelsTest {

    @Test
    fun timeSettingsDefaultToUtcPlus8Options() {
        val options = ForumTimeSettings().toNmbTimeFormatOptions()

        assertEquals(NmbTimeDisplayMode.RELATIVE, options.mode)
        assertEquals(false, options.showSeconds)
        assertEquals(NmbTimeZoneMode.UTC_PLUS_8, options.timeZoneMode)
    }

    @Test
    fun timeSettingsCanBuildLocalPreciseOptions() {
        val options = ForumTimeSettings(
            useUtcPlus8Time = false,
            usePreciseTime = true,
            showSeconds = true
        ).toNmbTimeFormatOptions()

        assertEquals(NmbTimeDisplayMode.PRECISE, options.mode)
        assertEquals(true, options.showSeconds)
        assertEquals(NmbTimeZoneMode.LOCAL, options.timeZoneMode)
    }
}
