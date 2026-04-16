package ink.duo3.fogisland.shared.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val APP_SUPPORT_DIRECTORY_NAME = "FogIsland"

@OptIn(ExperimentalForeignApi::class)
internal fun appleApplicationSupportFilePath(fileName: String): String {
    val fileManager = NSFileManager.defaultManager
    val applicationSupportDirectory = fileManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val basePath = requireNotNull(applicationSupportDirectory?.path) +
        "/$APP_SUPPORT_DIRECTORY_NAME"

    fileManager.createDirectoryAtPath(
        path = basePath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )

    return "$basePath/$fileName"
}
