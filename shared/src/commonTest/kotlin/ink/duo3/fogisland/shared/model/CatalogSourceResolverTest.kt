package ink.duo3.fogisland.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogSourceResolverTest {

    private val generalTimeline = Timeline(
        id = 1L,
        name = "综合线",
        displayName = "综合线",
        notice = "",
        maxPage = null
    )
    private val timeline = Timeline(
        id = 12L,
        name = "创作线",
        displayName = "创作线",
        notice = "测试",
        maxPage = null
    )
    private val boardGroup = ForumGroup(
        id = 100L,
        name = "综合版",
        sort = 0,
        status = "n",
        forums = listOf(
            ForumBoard(
                id = 4L,
                groupId = 100L,
                name = "综合版1",
                displayName = "时间胶囊",
                noticeHtml = "",
                noticeText = "",
                sort = 0,
                threadCount = null,
                permissionLevel = null,
                status = null
            )
        )
    )

    @Test
    fun preferredSourceIsResolvedAgainstCurrentCatalog() {
        val resolved = resolveCatalogSource(
            preferredSource = CatalogSource(CatalogType.FORUM, 4L, "", null),
            forumGroups = listOf(boardGroup),
            timelines = listOf(timeline),
            lastSelectedSource = null
        )

        assertEquals(boardGroup.forums.first().toCatalogSource(boardGroup), resolved)
    }

    @Test
    fun lastSelectedSourceIsUsedWhenPreferredIsMissing() {
        val resolved = resolveCatalogSource(
            preferredSource = CatalogSource(CatalogType.FORUM, 999L, "", null),
            forumGroups = listOf(boardGroup),
            timelines = listOf(timeline),
            lastSelectedSource = CatalogSource(CatalogType.TIMELINE, 12L, "", null)
        )

        assertEquals(timeline.toCatalogSource(), resolved)
    }

    @Test
    fun defaultSourceFallsBackToGeneralTimelineFirst() {
        val resolved = defaultCatalogSource(
            forumGroups = listOf(boardGroup),
            timelines = listOf(timeline, generalTimeline)
        )

        assertEquals(generalTimeline.toCatalogSource(), resolved)
    }

    @Test
    fun defaultSourceFallsBackToFirstForumWhenNoTimelineExists() {
        val resolved = defaultCatalogSource(
            forumGroups = listOf(boardGroup),
            timelines = emptyList()
        )

        assertEquals(boardGroup.forums.first().toCatalogSource(boardGroup), resolved)
    }

    @Test
    fun resolveForumDisplayNameUsesFallbackWhenMissing() {
        val forumNames = buildForumDisplayNameMap(listOf(boardGroup))

        assertEquals("时间胶囊", resolveForumDisplayName(4L, forumNames))
        assertEquals("板块 No.404", resolveForumDisplayName(404L, forumNames))
        assertNull(resolveForumDisplayName(null, forumNames))
    }
}
