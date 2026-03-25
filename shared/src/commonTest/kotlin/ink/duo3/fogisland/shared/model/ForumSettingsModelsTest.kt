package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.util.NmbTimeDisplayMode
import ink.duo3.fogisland.shared.util.NmbTimeZoneMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun cacheCleanupPolicyNeverHasNoTtl() {
        assertNull(CacheCleanupTtlPolicy.NEVER.ttlMillis())
    }

    @Test
    fun cacheCleanupPoliciesExposeExpectedTtlMillis() {
        assertEquals(7L * 24 * 60 * 60 * 1000, CacheCleanupTtlPolicy.ONE_WEEK.ttlMillis())
        assertEquals(30L * 24 * 60 * 60 * 1000, CacheCleanupTtlPolicy.ONE_MONTH.ttlMillis())
        assertEquals(92L * 24 * 60 * 60 * 1000, CacheCleanupTtlPolicy.THREE_MONTHS.ttlMillis())
        assertEquals(183L * 24 * 60 * 60 * 1000, CacheCleanupTtlPolicy.SIX_MONTHS.ttlMillis())
        assertEquals(365L * 24 * 60 * 60 * 1000, CacheCleanupTtlPolicy.ONE_YEAR.ttlMillis())
    }
}
