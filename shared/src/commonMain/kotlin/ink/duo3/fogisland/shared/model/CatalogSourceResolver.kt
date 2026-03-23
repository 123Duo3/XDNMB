package ink.duo3.fogisland.shared.model

private const val DEFAULT_TIMELINE_ID = 1L
private const val DEFAULT_TIMELINE_NAME = "综合线"

fun resolveCatalogSource(
    preferredSource: CatalogSource?,
    forumGroups: List<ForumGroup>,
    timelines: List<Timeline>,
    lastSelectedSource: CatalogSource?
): CatalogSource? {
    return findCatalogSource(
        source = preferredSource,
        forumGroups = forumGroups,
        timelines = timelines
    ) ?: findCatalogSource(
        source = lastSelectedSource,
        forumGroups = forumGroups,
        timelines = timelines
    ) ?: defaultCatalogSource(
        forumGroups = forumGroups,
        timelines = timelines
    )
}

fun defaultCatalogSource(
    forumGroups: List<ForumGroup>,
    timelines: List<Timeline>
): CatalogSource? {
    timelines.firstOrNull { timeline ->
        timeline.id == DEFAULT_TIMELINE_ID ||
            timeline.displayName == DEFAULT_TIMELINE_NAME ||
            timeline.name == DEFAULT_TIMELINE_NAME
    }?.let(Timeline::toCatalogSource)?.let { return it }

    timelines.firstOrNull()?.let(Timeline::toCatalogSource)?.let { return it }

    forumGroups.firstNotNullOfOrNull { group ->
        group.forums.firstOrNull()?.toCatalogSource(group)
    }?.let { return it }

    return null
}

fun CatalogSource.cacheKey(): String = "${type.name}:$id"

fun buildForumDisplayNameMap(forumGroups: List<ForumGroup>): Map<Long, String> {
    return forumGroups
        .flatMap { group -> group.forums.map { forum -> forum.id to forum.displayName } }
        .toMap()
}

fun resolveForumDisplayName(
    forumId: Long?,
    forumDisplayNames: Map<Long, String>
): String? {
    return forumId?.let(forumDisplayNames::get) ?: forumId?.let { "板块 No.$it" }
}

fun findCatalogSource(
    source: CatalogSource?,
    forumGroups: List<ForumGroup>,
    timelines: List<Timeline>
): CatalogSource? {
    return when (source?.type) {
        CatalogType.FORUM -> forumGroups.firstNotNullOfOrNull { group ->
            group.forums.firstOrNull { it.id == source.id }?.toCatalogSource(group)
        }

        CatalogType.TIMELINE -> timelines
            .firstOrNull { it.id == source.id }
            ?.toCatalogSource()

        null -> null
    }
}

fun ForumBoard.toCatalogSource(group: ForumGroup): CatalogSource {
    return CatalogSource(
        type = CatalogType.FORUM,
        id = id,
        title = displayName,
        subtitle = group.name
    )
}

fun Timeline.toCatalogSource(): CatalogSource {
    return CatalogSource(
        type = CatalogType.TIMELINE,
        id = id,
        title = displayName,
        subtitle = notice.takeIf { it.isNotBlank() }
    )
}
