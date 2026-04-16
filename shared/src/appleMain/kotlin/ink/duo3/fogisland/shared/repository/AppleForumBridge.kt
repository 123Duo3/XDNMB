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
 * Plain outcome type for the Apple bridge. Avoids generics and sealed classes
 * so Swift interop is trivial: `outcome.isSuccess`, `outcome.errorMessage`.
 *
 * SKIE's suspend-fn exception bridging crashes the process when a Kotlin
 * exception arrives while the Swift task is cancelling. To sidestep that,
 * every Apple-facing wrapper catches throwables on the Kotlin side and reports
 * them via this struct — nothing throws across the bridge except
 * CancellationException, which is always legitimate.
 */
data class AppleCallOutcome(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val errorKind: String?
) {
    companion object {
        val Success = AppleCallOutcome(isSuccess = true, errorMessage = null, errorKind = null)
        fun failure(message: String, kind: String) =
            AppleCallOutcome(isSuccess = false, errorMessage = message, errorKind = kind)
    }
}

/** Sidebar payload — always returned as a single struct for atomic UI update. */
data class AppleSidebarResult(
    val outcome: AppleCallOutcome,
    val timelines: List<Timeline>,
    val forumGroups: List<ForumGroup>
)

private fun Throwable.toOutcome(): AppleCallOutcome = when (this) {
    is NmbApiResponseException -> AppleCallOutcome.failure(
        message = message ?: "服务器返回错误",
        kind = "api"
    )
    is NmbApiException -> AppleCallOutcome.failure(
        message = message ?: "网络请求失败",
        kind = "network"
    )
    else -> AppleCallOutcome.failure(
        message = message ?: this::class.simpleName ?: "未知错误",
        kind = "unknown"
    )
}

/**
 * Apple-platform safe wrappers around [ForumRepository] and [ForumPreferences].
 * Swift should prefer these over calling shared types directly.
 */
class AppleForumBridge internal constructor(
    private val repository: ForumRepository,
    private val preferences: ForumPreferences
) {

    // ── Display utilities ────────────────────────────────────────────────────

    /** Builds the CDN thumb URL for a post image. Returns null for empty input. */
    fun buildThumbUrl(image: String?, ext: String?): String? =
        buildNmbThumbImageUrl(image = image, ext = ext)

    /** Resolves the API-provided backup CDN thumb URL after the primary image CDN fails. */
    suspend fun buildFallbackThumbUrl(image: String?, ext: String?, currentUrl: String?): String? {
        return try {
            val fallbackBaseUrl = repository.getImageCdnFallbackBaseUrl() ?: return null
            buildNmbThumbImageUrl(
                image = image,
                ext = ext,
                cdnBaseUrl = fallbackBaseUrl
            )?.takeUnless { it.isBlank() || it == currentUrl }
        } catch (t: CancellationException) {
            throw t
        } catch (_: Throwable) {
            null
        }
    }

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
    ): AppleCallOutcome {
        return try {
            repository.refreshCatalog(source, page, replaceLoadedPages)
            AppleCallOutcome.Success
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            t.toOutcome()
        }
    }

    suspend fun refreshThread(threadId: Long): AppleCallOutcome {
        return try {
            repository.refreshThread(threadId = threadId, page = 1)
            AppleCallOutcome.Success
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            t.toOutcome()
        }
    }

    suspend fun loadSidebar(): AppleSidebarResult {
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
            return AppleSidebarResult(
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
            return AppleSidebarResult(
                outcome = t.toOutcome(),
                timelines = freshTimelines,
                forumGroups = cached?.forumGroups ?: emptyList()
            )
        }

        return AppleSidebarResult(
            outcome = AppleCallOutcome.Success,
            timelines = freshTimelines,
            forumGroups = freshGroups
        )
    }
}
