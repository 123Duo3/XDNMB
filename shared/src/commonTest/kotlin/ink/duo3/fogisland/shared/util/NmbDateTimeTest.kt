package ink.duo3.fogisland.shared.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class NmbDateTimeTest {

    private fun postedAtEpochMillis(raw: String): Long {
        return parseNmbPostedAtEpochMillis(raw)
            ?: error("Expected '$raw' to be parsed")
    }

    @Test
    fun parseSupportsWeekdayWrappedFormat() {
        val parsed = parseNmbPostedAt("2026-03-24(二)02:55:51")
        assertNotNull(parsed)
        assertEquals("2026-03-23T18:55:51Z", parsed.toString())
    }

    @Test
    fun parseSupportsPlainFormat() {
        val parsed = parseNmbPostedAt("2099-01-01 00:00:01")
        assertNotNull(parsed)
        assertEquals("2098-12-31T16:00:01Z", parsed.toString())
    }

    @Test
    fun parseIgnoresParenthesizedText() {
        val parsed = parseNmbPostedAt("2026-03-24(随便什么)02:55:51")
        assertNotNull(parsed)
        assertEquals("2026-03-23T18:55:51Z", parsed.toString())
    }

    @Test
    fun relativeFormatterKeepsFutureText() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2026-03-24(二)14:38:00"),
            now = Instant.parse("2026-03-24T04:38:00Z")
        )

        assertNotNull(formatted)
        assertEquals("今天", formatted.dateText)
        assertEquals("2小时后", formatted.timeText)
        assertEquals("2小时后", formatted.displayText)
    }

    @Test
    fun relativeFormatterUsesJustNowWithinOneMinute() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2026-03-24(二)12:38:20"),
            now = Instant.parse("2026-03-24T04:38:00Z")
        )

        assertNotNull(formatted)
        assertEquals("今天", formatted.dateText)
        assertEquals("刚刚", formatted.timeText)
        assertEquals("刚刚", formatted.displayText)
    }

    @Test
    fun relativeFormatterUsesNamedDays() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2026-03-26(四)12:38:00"),
            now = Instant.parse("2026-03-24T04:38:00Z")
        )

        assertNotNull(formatted)
        assertEquals("后天", formatted.dateText)
        assertEquals("12:38", formatted.timeText)
        assertEquals("后天 12:38", formatted.displayText)
    }

    @Test
    fun preciseFormatterCanShowSeconds() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2025-02-01(六)12:38:09"),
            now = Instant.parse("2026-03-24T04:38:00Z"),
            options = NmbTimeFormatOptions(
                mode = NmbTimeDisplayMode.PRECISE,
                showSeconds = true
            )
        )

        assertNotNull(formatted)
        assertEquals("2025年2月1日", formatted.dateText)
        assertEquals("12:38:09", formatted.timeText)
        assertEquals("2025年2月1日 12:38:09", formatted.displayText)
    }

    @Test
    fun preciseFormatterKeepsMonthEvenInSameMonth() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2026-03-09(一)12:38:09"),
            now = Instant.parse("2026-03-24T04:38:00Z"),
            options = NmbTimeFormatOptions(
                mode = NmbTimeDisplayMode.PRECISE
            )
        )

        assertNotNull(formatted)
        assertEquals("3月9日", formatted.dateText)
        assertEquals("12:38", formatted.timeText)
        assertEquals("3月9日 12:38", formatted.displayText)
    }

    @Test
    fun localTimeZoneModeUsesDeviceTimeZoneForDisplay() {
        val formatted = formatNmbPostedAt(
            epochMillis = postedAtEpochMillis("2026-03-24(二)00:30:00"),
            now = Instant.parse("2026-03-23T15:00:00Z"),
            localTimeZone = TimeZone.of("UTC"),
            options = NmbTimeFormatOptions(
                mode = NmbTimeDisplayMode.RELATIVE,
                timeZoneMode = NmbTimeZoneMode.LOCAL
            )
        )

        assertNotNull(formatted)
        assertEquals("今天", formatted.dateText)
        assertEquals("1小时后", formatted.timeText)
        assertEquals("1小时后", formatted.displayText)
    }

    @Test
    fun parseCompactNoticeDateSupportsShortTimeSuffix() {
        val epochMillis = parseNmbNoticeDateEpochMillis(2026032400006)
            ?: error("Expected notice date to be parsed")

        assertEquals(
            "2026-03-23T16:00:06Z",
            Instant.fromEpochMilliseconds(epochMillis).toString()
        )
    }
}
