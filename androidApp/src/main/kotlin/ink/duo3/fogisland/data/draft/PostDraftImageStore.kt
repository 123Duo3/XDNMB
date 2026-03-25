package ink.duo3.fogisland.data.draft

import android.content.Context
import ink.duo3.fogisland.shared.model.ThreadPostImage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val POST_DRAFT_IMAGE_DIRECTORY = "post-draft-images"

suspend fun Context.persistDraftImage(
    image: ThreadPostImage,
    existingPath: String?
): String {
    return withContext(Dispatchers.IO) {
        val directory = postDraftImageDirectory().apply { mkdirs() }
        val targetFile = existingPath
            ?.let { resolveDraftImageFile(it) }
            ?: File(directory, "${UUID.randomUUID()}${image.fileName.fileExtensionOrEmpty()}")

        targetFile.writeBytes(image.bytes)
        targetFile.absolutePath
    }
}

suspend fun Context.readDraftImage(
    path: String,
    fileName: String?,
    mimeType: String?
): ThreadPostImage? {
    return withContext(Dispatchers.IO) {
        val file = resolveDraftImageFile(path) ?: return@withContext null
        if (!file.exists() || !file.isFile) {
            return@withContext null
        }

        ThreadPostImage(
            fileName = fileName?.takeIf { it.isNotBlank() } ?: file.name,
            mimeType = mimeType,
            bytes = file.readBytes()
        )
    }
}

suspend fun Context.deleteDraftImage(path: String?) {
    if (path.isNullOrBlank()) {
        return
    }

    withContext(Dispatchers.IO) {
        resolveDraftImageFile(path)?.let { file ->
            runCatching {
                file.delete()
            }
        }
    }
}

suspend fun Context.cleanupOrphanDraftImages(activePaths: Set<String>) {
    withContext(Dispatchers.IO) {
        val directory = postDraftImageDirectory()
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext
        }

        val activeCanonicalPaths = activePaths.mapNotNull { path ->
            resolveDraftImageFile(path)?.absolutePath
        }.toSet()

        directory.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filter { it.absolutePath !in activeCanonicalPaths }
            ?.forEach { file ->
                runCatching {
                    file.delete()
                }
            }
    }
}

private fun Context.postDraftImageDirectory(): File {
    return File(filesDir, POST_DRAFT_IMAGE_DIRECTORY)
}

private fun Context.resolveDraftImageFile(path: String): File? {
    val directory = runCatching { postDraftImageDirectory().canonicalFile }.getOrNull() ?: return null
    val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
    return file.takeIf { it.parentFile == directory }
}

private fun String.fileExtensionOrEmpty(): String {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex <= 0) "" else substring(dotIndex)
}
