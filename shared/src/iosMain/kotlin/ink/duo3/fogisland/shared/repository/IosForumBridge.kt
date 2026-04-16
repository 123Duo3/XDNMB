package ink.duo3.fogisland.shared.repository

import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.ForumTimeSettings
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.network.api.NmbApiException
import ink.duo3.fogisland.shared.network.api.NmbApiResponseException
import ink.duo3.fogisland.shared.storage.preferences.ForumPreferences
import ink.duo3.fogisland.shared.util.buildNmbThumbImageUrl
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.normalizeNmbStoredName
import ink.duo3.fogisland.shared.util.normalizeNmbStoredTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Plain outcome type for the iOS bridge. Avoids generics and sealed classes
 * so Swift interop is trivial: `outcome.isSuccess`, `outcome.errorMessage`.
 *
 * SKIE's suspend-fn exception bridging crashes the process when a Kotlin
 * exception arrives while the Swift task is cancelling. To sidestep that,
 * every iOS-facing wrapper catches throwables on the Kotlin side and reports
 * them via this struct — nothing throws across the bridge except
 * CancellationException, which is always legitimate.
 */
data class IosCallOutcome(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val errorKind: String?
) {
    companion object {
        val Success = IosCallOutcome(isSuccess = true, errorMessage = null, errorKind = null)
        fun failure(message: String, kind: String) =
            IosCallOutcome(isSuccess = false, errorMessage = message, errorKind = kind)
    }
}

/** Sidebar payload — always returned as a single struct for atomic UI update. */
data class IosSidebarResult(
    val outcome: IosCallOutcome,
    val timelines: List<Timeline>,
    val forumGroups: List<ForumGroup>
)

private fun Throwable.toOutcome(): IosCallOutcome = when (this) {
    is NmbApiResponseException -> IosCallOutcome.failure(
        message = message ?: "服务器返回错误",
        kind = "api"
    )
    is NmbApiException -> IosCallOutcome.failure(
        message = message ?: "网络请求失败",
        kind = "network"
    )
    else -> IosCallOutcome.failure(
        message = message ?: this::class.simpleName ?: "未知错误",
        kind = "unknown"
    )
}

/**
 * iOS-only safe wrappers around [ForumRepository] and [ForumPreferences].
 * Swift should prefer these over calling shared types directly.
 */
class IosForumBridge internal constructor(
    private val repository: ForumRepository,
    private val preferences: ForumPreferences
) {

    // ── Display utilities ────────────────────────────────────────────────────

    /** Builds the CDN thumb URL for a post image. Returns null for empty input. */
    fun buildThumbUrl(image: String?, ext: String?): String? =
        buildNmbThumbImageUrl(image = image, ext = ext)

    /**
     * Returns null when [title] is blank or the default sentinel "无标题".
     * Mirrors [normalizeNmbStoredTitle] from shared.
     */
    fun normalizeTitle(title: String?): String? = normalizeNmbStoredTitle(title)

    /**
     * Returns null when [name] is blank or the default sentinel "无名氏".
     * Mirrors [normalizeNmbStoredName] from shared.
     */
    fun normalizeName(name: String?): String? = normalizeNmbStoredName(name)

    // ── Time settings ────────────────────────────────────────────────────────

    /** Emits the current [ForumTimeSettings] and every subsequent change. */
    val timeSettingsFlow: Flow<ForumTimeSettings>
        get() = preferences.timeSettingsFlow

    suspend fun updateUseUtcPlus8Time(value: Boolean) =
        preferences.updateUseUtcPlus8Time(value)

    suspend fun updateUsePreciseTime(value: Boolean) =
        preferences.updateUsePreciseTime(value)

    suspend fun updateShowSeconds(value: Boolean) =
        preferences.updateShowSeconds(value)

    /**
     * Formats an epoch-millis timestamp using shared [formatNmbPostedAtText]
     * and the caller-supplied [ForumTimeSettings]. Safe to call on any thread;
     * returns an empty string when [epochMillis] is null.
     */
    fun formatTime(epochMillis: Long?, settings: ForumTimeSettings): String =
        formatNmbPostedAtText(
            epochMillis = epochMillis,
            options = settings.toNmbTimeFormatOptions()
        ) ?: ""

    // ── Network calls ────────────────────────────────────────────────────────

    suspend fun refreshCatalog(
        source: CatalogSource,
        page: Int,
        replaceLoadedPages: Boolean
    ): IosCallOutcome {
        return try {
            repository.refreshCatalog(source, page, replaceLoadedPages)
            IosCallOutcome.Success
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            t.toOutcome()
        }
    }

    suspend fun refreshThread(threadId: Long): IosCallOutcome {
        return try {
            repository.refreshThread(threadId = threadId, page = 1)
            IosCallOutcome.Success
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            t.toOutcome()
        }
    }

    suspend fun loadSidebar(): IosSidebarResult {
        val cached = try {
            repository.getCachedCatalogIndex()
        } catch (t: CancellationException) {
            throw t
        } catch (_: Throwable) {
            null
        }

        val freshTimelines = try {
            repository.getTimelineList()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            return IosSidebarResult(
                outcome = t.toOutcome(),
                timelines = cached?.timelines ?: emptyList(),
                forumGroups = cached?.forumGroups ?: emptyList()
            )
        }

        val freshGroups = try {
            repository.getForumList()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            return IosSidebarResult(
                outcome = t.toOutcome(),
                timelines = freshTimelines,
                forumGroups = cached?.forumGroups ?: emptyList()
            )
        }

        return IosSidebarResult(
            outcome = IosCallOutcome.Success,
            timelines = freshTimelines,
            forumGroups = freshGroups
        )
    }
}
