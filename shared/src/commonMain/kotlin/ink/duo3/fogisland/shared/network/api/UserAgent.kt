package ink.duo3.fogisland.shared.network.api

internal expect fun fogIslandPlatformName(): String

internal fun fogIslandUserAgent(): String {
    // TODO: Replace the placeholder version with the real app version once versioning is unified.
    return "FogIsland/0 (${fogIslandPlatformName()})"
}
