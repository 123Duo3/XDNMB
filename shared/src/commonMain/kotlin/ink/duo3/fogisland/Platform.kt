package ink.duo3.fogisland

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform