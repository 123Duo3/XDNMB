package ink.duo3.fogisland.shared.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant

private val nmbServerTimeZone = TimeZone.of("Asia/Shanghai")
private val nmbDateTimeRegex = Regex(
    pattern = """^\s*(\d{4})-(\d{1,2})-(\d{1,2})(?:\([^)]*\))?\s*(\d{1,2}):(\d{2})(?::(\d{2}))?\s*$"""
)

enum class NmbTimeDisplayMode {
    RELATIVE,
    PRECISE
}

enum class NmbTimeZoneMode {
    UTC_PLUS_8,
    LOCAL
}

data class NmbTimeFormatOptions(
    val mode: NmbTimeDisplayMode = NmbTimeDisplayMode.RELATIVE,
    val showSeconds: Boolean = false,
    val timeZoneMode: NmbTimeZoneMode = NmbTimeZoneMode.UTC_PLUS_8
)

data class NmbFormattedTime(
    val dateText: String,
    val timeText: String
) {
    val displayText: String
        get() = if (dateText == "今天") {
            timeText
        } else {
            "$dateText $timeText"
        }
}

fun parseNmbPostedAt(
    raw: String,
    timeZone: TimeZone = nmbServerTimeZone
): Instant? {
    val match = nmbDateTimeRegex.matchEntire(raw.trim()) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    val hour = match.groupValues[4].toIntOrNull() ?: return null
    val minute = match.groupValues[5].toIntOrNull() ?: return null
    val second = match.groupValues[6].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0

    return runCatching {
        val monthValue = Month.entries.getOrNull(month - 1)
            ?: return null

        LocalDateTime(
            year = year,
            month = monthValue,
            day = day,
            hour = hour,
            minute = minute,
            second = second
        ).toInstant(timeZone)
    }.getOrNull()
}

fun parseNmbPostedAtEpochMillis(
    raw: String,
    timeZone: TimeZone = nmbServerTimeZone
): Long? {
    return parseNmbPostedAt(raw, timeZone)?.toEpochMilliseconds()
}

fun parseNmbNoticeDateEpochMillis(
    raw: Long,
    timeZone: TimeZone = nmbServerTimeZone
): Long? {
    val rawText = raw.toString()

    parseCompactNoticeDateEpochMillis(rawText, timeZone)?.let { return it }

    return when {
        raw in 1_000_000_000L..9_999_999_999L -> raw * 1_000
        raw in 1_000_000_000_000L..9_999_999_999_999L -> raw
        else -> null
    }
}

fun formatNmbPostedAt(
    epochMillis: Long,
    now: Instant = currentInstant(),
    sourceTimeZone: TimeZone = nmbServerTimeZone,
    localTimeZone: TimeZone = currentSystemTimeZone(),
    options: NmbTimeFormatOptions = NmbTimeFormatOptions()
): NmbFormattedTime? {
    val targetInstant = Instant.fromEpochMilliseconds(epochMillis)
    val displayTimeZone = resolveDisplayTimeZone(
        sourceTimeZone = sourceTimeZone,
        localTimeZone = localTimeZone,
        mode = options.timeZoneMode
    )
    val targetDateTime = targetInstant.toLocalDateTime(displayTimeZone)
    val nowDateTime = now.toLocalDateTime(displayTimeZone)

    return NmbFormattedTime(
        dateText = formatDatePart(
            targetDate = targetDateTime.date,
            currentDate = nowDateTime.date,
            mode = options.mode
        ),
        timeText = formatTimePart(
            targetDateTime = targetDateTime,
            currentDateTime = nowDateTime,
            targetInstant = targetInstant,
            currentInstant = now,
            options = options
        )
    )
}

fun formatNmbPostedAtText(
    epochMillis: Long?,
    now: Instant = currentInstant(),
    sourceTimeZone: TimeZone = nmbServerTimeZone,
    localTimeZone: TimeZone = currentSystemTimeZone(),
    options: NmbTimeFormatOptions = NmbTimeFormatOptions()
): String? {
    val resolvedEpochMillis = epochMillis ?: return null
    return formatNmbPostedAt(
        epochMillis = resolvedEpochMillis,
        now = now,
        sourceTimeZone = sourceTimeZone,
        localTimeZone = localTimeZone,
        options = options
    )?.displayText
}

fun currentSystemTimeZone(): TimeZone {
    return currentSystemDefault()
}

private fun resolveDisplayTimeZone(
    sourceTimeZone: TimeZone,
    localTimeZone: TimeZone,
    mode: NmbTimeZoneMode
): TimeZone {
    return when (mode) {
        NmbTimeZoneMode.UTC_PLUS_8 -> sourceTimeZone
        NmbTimeZoneMode.LOCAL -> localTimeZone
    }
}

private fun formatDatePart(
    targetDate: LocalDate,
    currentDate: LocalDate,
    mode: NmbTimeDisplayMode
): String {
    if (mode == NmbTimeDisplayMode.RELATIVE) {
        return when ((targetDate.toEpochDays() - currentDate.toEpochDays()).toInt()) {
            2 -> "后天"
            1 -> "明天"
            0 -> "今天"
            -1 -> "昨天"
            -2 -> "前天"
            else -> formatPreciseDate(targetDate, currentDate)
        }
    }

    return formatPreciseDate(targetDate, currentDate)
}

private fun formatPreciseDate(
    targetDate: LocalDate,
    currentDate: LocalDate
): String {
    val targetMonth = targetDate.month.ordinal + 1
    return when {
        targetDate.year != currentDate.year ->
            "${targetDate.year}年${targetMonth}月${targetDate.day}日"

        else -> "${targetMonth}月${targetDate.day}日"
    }
}

private fun formatTimePart(
    targetDateTime: LocalDateTime,
    currentDateTime: LocalDateTime,
    targetInstant: Instant,
    currentInstant: Instant,
    options: NmbTimeFormatOptions
): String {
    if (options.mode == NmbTimeDisplayMode.PRECISE) {
        return formatClock(
            dateTime = targetDateTime,
            showSeconds = options.showSeconds,
            padHour = true
        )
    }

    if (targetDateTime.date == currentDateTime.date) {
        val diffSeconds = (targetInstant - currentInstant).inWholeSeconds
        val relativeText = formatRelativeDelta(diffSeconds)
        if (relativeText != null) {
            return relativeText
        }
    }

    return formatClock(
        dateTime = targetDateTime,
        showSeconds = false,
        padHour = false
    )
}

private fun formatRelativeDelta(diffSeconds: Long): String? {
    val absSeconds = diffSeconds.absoluteValue
    if (absSeconds < 60) {
        return "刚刚"
    }

    val suffix = if (diffSeconds >= 0) "后" else "前"

    val amount = when {
        absSeconds < 60 * 60 -> maxOf(absSeconds / 60, 1L).toInt() to "分钟"
        absSeconds < 6 * 60 * 60 -> maxOf(absSeconds / (60 * 60), 1L).toInt() to "小时"
        else -> return null
    }

    return "${amount.first}${amount.second}$suffix"
}

private fun formatClock(
    dateTime: LocalDateTime,
    showSeconds: Boolean,
    padHour: Boolean
): String {
    val hour = if (padHour) {
        dateTime.hour.toString().padStart(2, '0')
    } else {
        dateTime.hour.toString()
    }
    val minute = dateTime.minute.toString().padStart(2, '0')
    if (!showSeconds) {
        return "$hour:$minute"
    }

    val second = dateTime.second.toString().padStart(2, '0')
    return "$hour:$minute:$second"
}

private fun currentInstant(): Instant {
    return Clock.System.now()
}

private fun parseCompactNoticeDateEpochMillis(
    raw: String,
    timeZone: TimeZone
): Long? {
    if (raw.length !in 8..14 || !raw.all(Char::isDigit)) {
        return null
    }

    val year = raw.substring(0, 4).toIntOrNull() ?: return null
    val month = raw.substring(4, 6).toIntOrNull() ?: return null
    val day = raw.substring(6, 8).toIntOrNull() ?: return null
    val timeText = raw.drop(8).padStart(6, '0')

    val hour = timeText.substring(0, 2).toIntOrNull() ?: return null
    val minute = timeText.substring(2, 4).toIntOrNull() ?: return null
    val second = timeText.substring(4, 6).toIntOrNull() ?: return null

    return runCatching {
        val monthValue = Month.entries.getOrNull(month - 1)
            ?: return null

        LocalDateTime(
            year = year,
            month = monthValue,
            day = day,
            hour = hour,
            minute = minute,
            second = second
        ).toInstant(timeZone).toEpochMilliseconds()
    }.getOrNull()
}
