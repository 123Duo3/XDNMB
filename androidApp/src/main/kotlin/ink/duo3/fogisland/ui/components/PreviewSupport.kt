package ink.duo3.fogisland.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalThemeSettings
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.data.ThemeSettings
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.DirectThreadShortcut
import ink.duo3.fogisland.shared.model.ForumBoard
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.ForumTimeSettings
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SearchHitType
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.ui.theme.FogIslandTheme

internal object NmbPreviewImages {
    const val Normal = "preview:normal"
    const val Wide = "preview:wide"
    const val Tall = "preview:tall"
    const val Long = "preview:long"
}

private val PreviewThemeSettings = ThemeSettings(
    followSystemAppearance = false,
    useDarkMode = false,
    useMonet = false
)

private const val PreviewNow = 1_712_345_678_000L
private const val PreviewBodyText =
    "这是一段用于 Preview 的正文，方便直接在 IDE 里调整卡片层级、间距和行数。第二行可以顺手看换行效果。"
private const val PreviewReplyText =
    "这是一条回复预览，用来看平铺样式、分割线和正文密度。"

@Composable
internal fun FogIslandPreviewColumn(
    modifier: Modifier = Modifier,
    verticalSpacingDp: Int = 12,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalThemeSettings provides PreviewThemeSettings,
        LocalTimeSettings provides ForumTimeSettings()
    ) {
        FogIslandTheme(themeSettings = PreviewThemeSettings) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacingDp.dp),
                    content = content
                )
            }
        }
    }
}

internal object NmbPreviewSamples {
    val forumThread = NmbPost(
        id = 45678901L,
        forumId = 4L,
        userHash = "A1B2C3",
        name = "匿名",
        title = "今天岛上风很大",
        contentHtml = "",
        contentText = PreviewBodyText,
        image = NmbPreviewImages.Wide,
        ext = "jpg",
        postedAtEpochMillis = PreviewNow,
        sage = false,
        admin = false,
        hide = false,
        isTips = false,
        isPoster = true,
        replyCount = 37,
        remainReplies = 12,
        refreshedAt = PreviewNow
    )

    val forumThreadWithoutTitle = forumThread.copy(
        id = 45678902L,
        title = null,
        contentText = "没有标题时也要能直接看正文和图片的位置。",
        image = NmbPreviewImages.Tall
    )

    val forumThreadWithoutImage = forumThread.copy(
        id = 45678903L,
        title = "只有正文的串",
        contentText = "这个场景用来看没有图片时，正文和 footer 之间的距离。",
        image = null,
        ext = null
    )

    val subscriptionThread = NmbPost(
        id = 45678911L,
        forumId = 11L,
        userHash = "SUB123",
        name = "匿名",
        title = "订阅串标题",
        contentHtml = "",
        contentText = PreviewBodyText,
        image = NmbPreviewImages.Normal,
        ext = "jpg",
        postedAtEpochMillis = PreviewNow,
        sage = false,
        admin = false,
        hide = false,
        isTips = false,
        isPoster = true,
        replyCount = 12,
        remainReplies = 4,
        refreshedAt = PreviewNow
    )

    val historyThread = NmbPost(
        id = 45678921L,
        forumId = 6L,
        userHash = "HIS888",
        name = "匿名",
        title = "历史记录里的串标题",
        contentHtml = "",
        contentText = "这条历史记录没有图片，主要用来看信息密度和按钮位置。",
        image = null,
        ext = null,
        postedAtEpochMillis = PreviewNow - 86_400_000L,
        sage = false,
        admin = false,
        hide = false,
        isTips = false,
        isPoster = true,
        replyCount = 28,
        remainReplies = null,
        refreshedAt = PreviewNow
    )

    val directThreadShortcutCached = DirectThreadShortcut(
        threadId = 45678931L,
        forumId = 2L,
        userHash = "DIR001",
        name = "匿名",
        title = "缓存里已有的直达串",
        preview = PreviewBodyText,
        postedAtEpochMillis = PreviewNow - 3_600_000L,
        isCached = true
    )

    val directThreadShortcutUncached = directThreadShortcutCached.copy(
        threadId = 45678932L,
        title = "本地还没缓存的直达串",
        preview = "",
        isCached = false
    )

    val searchThreadHit = SearchHit(
        type = SearchHitType.THREAD,
        threadId = 45678941L,
        postId = null,
        forumId = 10L,
        userHash = "SEA123",
        name = "匿名",
        title = "搜索命中的主串标题",
        preview = "这是主串里命中的正文片段，里面包含关键词“岛风”。",
        postedAtEpochMillis = PreviewNow - 7_200_000L,
        sage = true,
        admin = false,
        hide = false,
        page = null,
        refreshedAt = PreviewNow
    )

    val searchReplyHit = SearchHit(
        type = SearchHitType.POST,
        threadId = 45678941L,
        postId = 45678944L,
        forumId = 10L,
        userHash = "SEA456",
        name = "匿名",
        title = null,
        preview = "这是回复里命中的正文片段，也包含关键词“岛风”。",
        postedAtEpochMillis = PreviewNow - 1_800_000L,
        sage = false,
        admin = true,
        hide = false,
        page = 3,
        refreshedAt = PreviewNow
    )

    val replyPost = NmbPost(
        threadId = forumThread.id,
        id = 45678951L,
        remoteId = 45678951L,
        forumId = forumThread.forumId,
        replyCount = null,
        userHash = "REP123",
        name = "匿名",
        title = null,
        contentHtml = "",
        contentText = PreviewReplyText,
        image = NmbPreviewImages.Long,
        ext = "jpg",
        postedAtEpochMillis = PreviewNow - 900_000L,
        sage = false,
        admin = false,
        hide = false,
        isTips = false,
        isPoster = false,
        isThread = false,
        page = 2,
        positionInPage = 8,
        refreshedAt = PreviewNow
    )

    val forumGroups = listOf(
        ForumGroup(
            id = 1L,
            name = "综合",
            sort = 1,
            status = "n",
            forums = listOf(
                ForumBoard(
                    id = 4L,
                    groupId = 1L,
                    name = "欢乐恶搞",
                    displayName = "欢乐恶搞",
                    noticeHtml = "",
                    noticeText = "",
                    sort = 1,
                    threadCount = null,
                    permissionLevel = null,
                    status = "n"
                ),
                ForumBoard(
                    id = 6L,
                    groupId = 1L,
                    name = "综合版",
                    displayName = "综合版",
                    noticeHtml = "",
                    noticeText = "",
                    sort = 2,
                    threadCount = null,
                    permissionLevel = null,
                    status = "n"
                )
            )
        )
    )

    val forumSource = CatalogSource(
        type = CatalogType.FORUM,
        id = 4L,
        title = "欢乐恶搞",
        subtitle = "串列表预览"
    )

    val siteNotice = SiteNotice(
        contentHtml = "",
        contentText = "这是公告预览，用来看站点公告和串列表一起出现时的层次关系。",
        publishedAt = PreviewNow - 3_600_000L
    )
}
