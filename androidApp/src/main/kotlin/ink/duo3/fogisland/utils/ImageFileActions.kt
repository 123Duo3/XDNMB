package ink.duo3.fogisland.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

internal sealed interface ImageDownloadResult {
    data class Saved(val uri: Uri) : ImageDownloadResult
    data class Enqueued(val downloadId: Long) : ImageDownloadResult
}

private enum class ImageViewerExportFormat(
    val fileExtension: String,
    val mimeType: String
) {
    Jpeg(
        fileExtension = "jpg",
        mimeType = "image/jpeg"
    ),
    Png(
        fileExtension = "png",
        mimeType = "image/png"
    ),
    Webp(
        fileExtension = "webp",
        mimeType = "image/webp"
    )
}

internal suspend fun shareBitmapImage(
    context: Context,
    bitmap: Bitmap,
    imageId: String,
    ext: String?,
    imageUrl: String?
): Result<Intent> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val exportFormat = resolveImageViewerExportFormat(
                ext = ext,
                imageUrl = imageUrl
            )
            val fileName = buildImageViewerFileName(
                imageId = imageId,
                exportFormat = exportFormat
            )
            val directory = File(context.cacheDir, "shared_images").apply {
                mkdirs()
            }
            val file = File(directory, fileName)
            file.outputStream().use { output ->
                writeImageViewerBitmap(
                    bitmap = bitmap,
                    exportFormat = exportFormat,
                    output = output
                )
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = exportFormat.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(
                    context.contentResolver,
                    fileName,
                    uri
                )
            }
        }
    }
}

internal suspend fun downloadBitmapImage(
    context: Context,
    bitmap: Bitmap,
    imageId: String,
    ext: String?,
    imageUrl: String?
): Result<ImageDownloadResult> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val exportFormat = resolveImageViewerExportFormat(
                ext = ext,
                imageUrl = imageUrl
            )
            val fileName = buildImageViewerFileName(
                imageId = imageId,
                exportFormat = exportFormat
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val request = DownloadManager.Request(Uri.parse(imageUrl))
                    .setTitle(fileName)
                    .setMimeType(exportFormat.mimeType)
                    .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_PICTURES,
                        "FogIsland/$fileName"
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                val downloadManager = context.getSystemService(DownloadManager::class.java)
                    ?: error("下载服务不可用")
                return@runCatching ImageDownloadResult.Enqueued(
                    downloadId = downloadManager.enqueue(request)
                )
            }

            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, exportFormat.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FogIsland")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("无法创建图片文件")

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    writeImageViewerBitmap(
                        bitmap = bitmap,
                        exportFormat = exportFormat,
                        output = output
                    )
                } ?: error("无法写入图片文件")

                val completedValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, completedValues, null, null)
                ImageDownloadResult.Saved(uri = uri)
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
        }
    }
}

private fun resolveImageViewerExportFormat(
    ext: String?,
    imageUrl: String?
): ImageViewerExportFormat {
    val normalizedExtension = ext
        ?.substringAfterLast('.')
        ?.ifBlank { null }
        ?: imageUrl
            ?.substringBefore('?')
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.ifBlank { null }

    return when (normalizedExtension?.lowercase()) {
        "png" -> ImageViewerExportFormat.Png
        "webp" -> ImageViewerExportFormat.Webp
        else -> ImageViewerExportFormat.Jpeg
    }
}

private fun buildImageViewerFileName(
    imageId: String,
    exportFormat: ImageViewerExportFormat
): String {
    val normalizedId = imageId
        .trim()
        .substringAfterLast('/')
        .substringBefore('?')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_', '.')
        .ifEmpty { "image" }
    return "$normalizedId.${exportFormat.fileExtension}"
}

private fun writeImageViewerBitmap(
    bitmap: Bitmap,
    exportFormat: ImageViewerExportFormat,
    output: OutputStream
) {
    val compressFormat = when (exportFormat) {
        ImageViewerExportFormat.Jpeg -> Bitmap.CompressFormat.JPEG
        ImageViewerExportFormat.Png -> Bitmap.CompressFormat.PNG
        ImageViewerExportFormat.Webp -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.PNG
            }
        }
    }
    val success = bitmap.compress(
        compressFormat,
        100,
        output
    )
    if (!success) {
        error("图片导出失败")
    }
}
