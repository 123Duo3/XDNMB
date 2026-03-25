package ink.duo3.fogisland.shared.util

fun escapeSqlLikeArgument(query: String): String {
    return query
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
