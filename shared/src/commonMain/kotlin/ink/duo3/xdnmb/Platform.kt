package ink.duo3.xdnmb

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform