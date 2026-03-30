package ink.duo3.fogisland.utils

import android.app.DownloadManager
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.net.URL

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
    Gif(
        fileExtension = "gif",
        mimeType = "image/gif"
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

internal suspend fun shareRemoteImage(
    context: Context,
    imageId: String,
    ext: String?,
    imageUrl: String,
    diskCacheKey: String?,
    bitmapFallback: Bitmap?
): Result<Intent> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val exportFormat = resolveImageViewerExportFormat(
                ext = ext,
                imageUrl = imageUrl
            )
            val cachedBytes = diskCacheKey?.let { key ->
                readCachedImageBytes(context, key)
            }

            when {
                cachedBytes != null -> {
                    shareImageBytes(
                        context = context,
                        bytes = cachedBytes,
                        imageId = imageId,
                        exportFormat = exportFormat
                    )
                }
                bitmapFallback != null && exportFormat != ImageViewerExportFormat.Gif -> {
                    shareBitmapImage(
                        context = context,
                        bitmap = bitmapFallback,
                        imageId = imageId,
                        ext = ext,
                        imageUrl = imageUrl
                    ).getOrThrow()
                }
                else -> {
                    shareImageBytes(
                        context = context,
                        bytes = URL(imageUrl).openStream().use { it.readBytes() },
                        imageId = imageId,
                        exportFormat = exportFormat
                    )
                }
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

internal suspend fun downloadRemoteImage(
    context: Context,
    imageId: String,
    ext: String?,
    imageUrl: String,
    diskCacheKey: String?,
    bitmapFallback: Bitmap?
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
            val cachedBytes = diskCacheKey?.let { key ->
                readCachedImageBytes(context, key)
            }

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

            when {
                cachedBytes != null -> {
                    saveImageBytesQPlus(
                        context = context,
                        bytes = cachedBytes,
                        fileName = fileName,
                        exportFormat = exportFormat
                    )
                }
                bitmapFallback != null && exportFormat != ImageViewerExportFormat.Gif -> {
                    downloadBitmapImage(
                        context = context,
                        bitmap = bitmapFallback,
                        imageId = imageId,
                        ext = ext,
                        imageUrl = imageUrl
                    ).getOrThrow()
                }
                else -> {
                    saveImageBytesQPlus(
                        context = context,
                        bytes = URL(imageUrl).openStream().use { it.readBytes() },
                        fileName = fileName,
                        exportFormat = exportFormat
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
private fun readCachedImageBytes(
    context: Context,
    diskCacheKey: String
): ByteArray? {
    val diskCache = context.imageLoader.diskCache ?: return null
    val snapshot = diskCache.openSnapshot(diskCacheKey) ?: return null
    return snapshot.use {
        File(snapshot.data.toString()).takeIf { file -> file.exists() }?.readBytes()
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
        "gif" -> ImageViewerExportFormat.Gif
        "png" -> ImageViewerExportFormat.Png
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

private fun shareImageBytes(
    context: Context,
    bytes: ByteArray,
    imageId: String,
    exportFormat: ImageViewerExportFormat
): Intent {
    val fileName = buildImageViewerFileName(
        imageId = imageId,
        exportFormat = exportFormat
    )
    val directory = File(context.cacheDir, "shared_images").apply {
        mkdirs()
    }
    val file = File(directory, fileName)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    return Intent(Intent.ACTION_SEND).apply {
        type = exportFormat.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(
            context.contentResolver,
            fileName,
            uri
        )
    }
}

private fun saveImageBytesQPlus(
    context: Context,
    bytes: ByteArray,
    fileName: String,
    exportFormat: ImageViewerExportFormat
): ImageDownloadResult {
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
            output.write(bytes)
        } ?: error("无法写入图片文件")

        val completedValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, completedValues, null, null)
        return ImageDownloadResult.Saved(uri = uri)
    } catch (throwable: Throwable) {
        resolver.delete(uri, null, null)
        throw throwable
    }
}

private fun writeImageViewerBitmap(
    bitmap: Bitmap,
    exportFormat: ImageViewerExportFormat,
    output: OutputStream
) {
    val compressFormat = when (exportFormat) {
        ImageViewerExportFormat.Jpeg -> Bitmap.CompressFormat.JPEG
        ImageViewerExportFormat.Png -> Bitmap.CompressFormat.PNG
        ImageViewerExportFormat.Gif -> error("GIF 不支持位图导出")
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
