package ink.duo3.xdnmb.android.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AppViewModel(
    private val sdk: XdSDK
) : ViewModel() {
    sealed class ForumListState {
        object Loading : ForumListState()
        data class OK(
            val forums: List<ForumGroup> // TODO: Use custom model
        ) : ForumListState()

        data class Error(val message: String) : ForumListState()
    }

    sealed class ThreadListState {
        object Loading : ThreadListState()
        object Refreshing : ThreadListState()
        data class OK(
            val forumId: Int,
            val threads: List<Thread> // TODO: Use custom model
        ) : ThreadListState()

        data class Error(val message: String) : ThreadListState()
    }

    val forumList = MutableStateFlow<ForumListState>(ForumListState.Loading)
    val timeLine = MutableStateFlow<ThreadListState>(ThreadListState.Loading)

    private var forumId: Int = -1

    private suspend fun getForumList() {
        try {
            forumList.value = ForumListState.OK(sdk.getForumList(false))
        } catch (e: Exception) {
            forumList.value = ForumListState.Error(e.message ?: "Unknown Error")
        }
    }

    private suspend fun getThreads() {
        try {
            timeLine.value = ThreadListState.OK(
                forumId,
                if (forumId == -1) {
                    sdk.getTimeLine(true, 1)
                } else {
                    sdk.getForumThreads(forumId, true, 1)
                }
            )
        } catch (e: Exception) {
            timeLine.value = ThreadListState.Error(e.message ?: "Unknown Error")
        }
    }

    /**
     * ????????????
     */
    fun init() {
        viewModelScope.launch {
            launch {
                forumList.value = ForumListState.Loading
                getForumList()
            }
            launch {
                timeLine.value = ThreadListState.Loading
                getThreads()
            }
            joinAll()
        }
    }

    /**
     * ???????????????
     */
    fun refreshThreads() {
        viewModelScope.launch {
            timeLine.value = ThreadListState.Refreshing
            getThreads()
        }
    }

    /**
     * ??????????????? Forum
     */
    fun setForumId(id: Int) {
        viewModelScope.launch {
            forumId = id
            // ??????????????? Threads
            timeLine.value = ThreadListState.Loading
            getThreads()
        }
    }
}

// TODO: Remove this
@Stable
fun formatTime(originalTime: String, inThread: Boolean): String {
    val timeZone = TimeZone.of("UTC+08:00")
    val originalTimeInISO = originalTime.replace(Regex("\\((.+?)\\)"), "T")
    val time = LocalDateTime.parse(originalTimeInISO)
    val currentInstant = Clock.System.now()
    val current = currentInstant.toLocalDateTime(timeZone)
    val timeInstant = time.toInstant(timeZone)

    val date = time.date.atTime(12, 0, 0, 0).toInstant(timeZone)
    val currentDate = currentInstant.toLocalDateTime(timeZone).date
        .atTime(12, 0, 0, 0).toInstant(timeZone)
    val diffInDay = date.periodUntil(currentDate, timeZone)
    val duration = currentInstant - timeInstant
    var result: String

    result = if (diffInDay.days < 1) {
        if (duration.inWholeHours < 1) {
            if (duration.inWholeMinutes < 1) {
                duration.inWholeSeconds.toString() + "??????"
            } else {
                duration.inWholeMinutes.toString() + "?????????"
            }
        } else {
            duration.inWholeHours.toString() + "?????????"
        }
    } else {
        when (diffInDay.days) {
            -2 -> "??????"
            -1 -> "??????"
            1 -> "??????"
            2 -> "??????"
            else -> if (time.year == current.year) {
                time.monthNumber.toString() + "???" + time.dayOfMonth + "???"
            } else {
                time.year.toString() + "???" + time.monthNumber + "???" + time.dayOfMonth + "???"
            }
        }
    }

    if (inThread) {
        if (diffInDay.days >= 1 || diffInDay.months != 0) {
            result =
                result + " " + time.hour.let { if (it < 10) "0$it" else it } + ":" + time.minute.let { if (it < 10) "0$it" else it }
        } else if (duration.inWholeHours >= 1) {
            result = time.hour.let { if (it < 10) "0$it" else it.toString() } + ":" + time.minute.let { if (it < 10) "0$it" else it }
        }
    }

    return result
}