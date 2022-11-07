package ink.duo3.xdnmb.shared.network

import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.cache.DatabaseDriverFactory
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test

internal class XdSDKTest {
    @Test
    fun formatTimeTest() {
        println(formatTime("2022-11-04(六)21:34:29",true))
    }
    private fun formatTime(originalTime: String, inThread: Boolean): String {
        val timeZone = TimeZone.of("UTC+08:00")
        val originalTime = originalTime.replace(Regex("\\((.+?)\\)") , "T")
        val time = LocalDateTime.parse(originalTime)
        val currentInstant = Clock.System.now()
        val current = currentInstant.toLocalDateTime(timeZone)
        val timeInstant = time.toInstant(timeZone)

        val date = time.date.atTime(12,0,0,0).toInstant(timeZone)
        val currentDate = currentInstant.toLocalDateTime(timeZone).date
            .atTime(12,0,0,0).toInstant(timeZone)
        val diffInDay = date.periodUntil(currentDate, timeZone)
        val duration = currentInstant - timeInstant
        var result = ""

        result = if (diffInDay.days < 1 ) {
            if (duration.inWholeHours < 1) {
                if (duration.inWholeMinutes <= 1) {
                    duration.inWholeSeconds.toString() + "秒前"
                } else {
                    duration.inWholeMinutes.toString() + "分钟前"
                }
            } else {
                duration.inWholeHours.toString() + "小时前"
            }
        } else {
            when (diffInDay.days) {
                -2 -> "后天"
                -1 -> "明天"
                1 -> "昨天"
                2 -> "前天"
                else -> if (time.year == current.year) {
                    time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
                } else {
                    time.year.toString() + "年" + time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
                }
            }
        }

        if (inThread) {
            if (diffInDay.days >= 1){
                result = result + " " + time.hour + ":" + time.minute
            } else if (duration.inWholeHours >= 1) {
                result = time.hour.toString() + ":" + time.minute.toString()
            }
        }

        return result
    }
}