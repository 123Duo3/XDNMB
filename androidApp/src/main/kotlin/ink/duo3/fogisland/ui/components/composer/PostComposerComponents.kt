package ink.duo3.fogisland.ui.components.composer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.shared.model.CookieProfile
import ink.duo3.fogisland.shared.model.ThreadPostImage
import ink.duo3.fogisland.shared.util.MAX_POST_IMAGE_BYTES
import ink.duo3.fogisland.shared.util.MAX_POST_IMAGE_LABEL
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TARGET_POST_IMAGE_BYTES = MAX_POST_IMAGE_BYTES
private const val MAX_JPEG_QUALITY = 95
private const val MIN_JPEG_QUALITY = 35
private const val MAX_IMAGE_RESAMPLE_ATTEMPTS = 6
private val supportedPostImageMimeTypes = setOf("image/jpeg", "image/png", "image/gif")

@Composable
fun ActivePostCookieCard(
    cookie: CookieProfile,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "当前发言饼干",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = cookie.displayName ?: "未命名饼干",
                style = MaterialTheme.typography.bodyLarge
            )
            if (cookie.remark.isNotBlank()) {
                Text(
                    text = cookie.remark,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PostDraftFields(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    isContentInvalid: Boolean,
    modifier: Modifier = Modifier,
    contentPlaceholder: String = "正文可留空，但正文和图片至少要有一个",
    contentInvalidMessage: String = "正文和图片至少要有一个"
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("名称") },
            placeholder = { Text("可留空") }
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("邮箱") },
            placeholder = { Text("可留空") }
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("标题") },
            placeholder = { Text("可留空") }
        )

        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("正文") },
            placeholder = { Text(contentPlaceholder) },
            isError = isContentInvalid,
            supportingText = {
                if (isContentInvalid) {
                    Text(contentInvalidMessage)
                }
            },
            minLines = 8,
            maxLines = 16
        )
    }
}

@Composable
fun PostImageCard(
    selectedImage: ThreadPostImage?,
    useWatermark: Boolean,
    isImageTooLarge: Boolean,
    isPreparingImage: Boolean,
    onPickImage: () -> Unit,
    onPreviewImage: (() -> Unit)?,
    onRemoveImage: () -> Unit,
    onUseWatermarkChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "图片",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = selectedImage?.fileName ?: "未选择图片",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = if (selectedImage != null) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedImage != null && isImageTooLarge) {
                        Text(
                            text = "超过 $MAX_POST_IMAGE_LABEL，可能无法发送",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onPickImage,
                        enabled = !isPreparingImage
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text("选择")
                    }
                    if (selectedImage != null && onPreviewImage != null) {
                        OutlinedButton(
                            onClick = onPreviewImage,
                            enabled = !isPreparingImage
                        ) {
                            Text("预览")
                        }
                    }
                    if (selectedImage != null) {
                        IconButton(onClick = onRemoveImage) {
                            Icon(Icons.Default.Close, contentDescription = "移除图片")
                        }
                    }
                }
            }

            if (selectedImage != null) {
                if (isPreparingImage) {
                    Text(
                        text = "正在优化图片体积…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseWatermarkChange(!useWatermark) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "启用水印",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "对应表单里的 water 选项。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = useWatermark,
                        onCheckedChange = onUseWatermarkChange
                    )
                }
            }
        }
    }
}

suspend fun Context.readPostImage(uri: Uri): ThreadPostImage {
    return withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(uri)
            ?: uri.lastPathSegment
            ?: "image"
        val mimeType = contentResolver.getType(uri)
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalStateException("无法读取所选图片")

        ThreadPostImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }
}

suspend fun Context.preparePostImageForSending(image: ThreadPostImage): ThreadPostImage {
    return withContext(Dispatchers.Default) {
        val normalizedImage = if (image.isSupportedPostImageFormat()) {
            image
        } else {
            image.normalizeToSupportedPostImage()
        }

        if (normalizedImage.isGifImage()) {
            return@withContext normalizedImage
        }

        if (normalizedImage.bytes.size <= TARGET_POST_IMAGE_BYTES) {
            return@withContext normalizedImage
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(
            normalizedImage.bytes,
            0,
            normalizedImage.bytes.size
        )
            ?: throw IllegalStateException("无法解码所选图片")
        val orientedBitmap = decodedBitmap.rotateByExifOrientation(normalizedImage.bytes)
        val jpegReadyBitmap = orientedBitmap.ensureOpaqueForJpeg()
        var workingBitmap = jpegReadyBitmap

        try {
            repeat(MAX_IMAGE_RESAMPLE_ATTEMPTS) {
                val attempt = workingBitmap.compressToTargetBytes(TARGET_POST_IMAGE_BYTES)
                attempt.bytes?.let { compressedBytes ->
                    return@withContext ThreadPostImage(
                        fileName = normalizedImage.fileName.toJpegFileName(),
                        mimeType = "image/jpeg",
                        bytes = compressedBytes
                    )
                }

                val nextScale = calculateCompressionScale(
                    currentBytes = attempt.smallestAttemptBytes,
                    targetBytes = TARGET_POST_IMAGE_BYTES
                )
                val scaledBitmap = workingBitmap.scaledForCompression(nextScale)
                if (scaledBitmap === workingBitmap) {
                    return@repeat
                }
                if (workingBitmap !== jpegReadyBitmap) {
                    workingBitmap.recycle()
                }
                workingBitmap = scaledBitmap
            }

            val finalAttempt = workingBitmap.compressToTargetBytes(MAX_POST_IMAGE_BYTES)
            finalAttempt.bytes?.let { compressedBytes ->
                return@withContext ThreadPostImage(
                    fileName = normalizedImage.fileName.toJpegFileName(),
                    mimeType = "image/jpeg",
                    bytes = compressedBytes
                )
            }

            normalizedImage
        } finally {
            if (workingBitmap !== jpegReadyBitmap) {
                workingBitmap.recycle()
            }
            if (jpegReadyBitmap !== orientedBitmap) {
                jpegReadyBitmap.recycle()
            }
            if (orientedBitmap !== decodedBitmap) {
                orientedBitmap.recycle()
            }
            decodedBitmap.recycle()
        }
    }
}

fun shouldPreparePostImageForSending(image: ThreadPostImage?): Boolean {
    if (image == null) {
        return false
    }
    if (image.isGifImage()) {
        return !image.isSupportedPostImageFormat()
    }
    return !image.isSupportedPostImageFormat() ||
        image.bytes.size > TARGET_POST_IMAGE_BYTES
}

fun ThreadPostImage.hasSamePayloadAs(other: ThreadPostImage): Boolean {
    return fileName == other.fileName &&
        mimeType == other.mimeType &&
        bytes.contentEquals(other.bytes)
}

private fun Context.queryDisplayName(uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex < 0 || !cursor.moveToFirst()) {
            null
        } else {
            cursor.getString(nameIndex)
        }
    }?.takeIf { it.isNotBlank() }
}

private fun Bitmap.rotateByExifOrientation(sourceBytes: ByteArray): Bitmap {
    val orientation = ByteArrayInputStream(sourceBytes).use { inputStream ->
        ExifInterface(inputStream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }

    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            else -> Unit
        }
    }

    if (matrix.isIdentity) {
        return this
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.ensureOpaqueForJpeg(): Bitmap {
    if (!hasAlpha()) {
        return this
    }

    val flattenedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(flattenedBitmap)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(this, 0f, 0f, null)
    return flattenedBitmap
}

private fun Bitmap.scaledForCompression(scale: Float): Bitmap {
    if (scale >= 0.999f) {
        return this
    }

    val targetWidth = max(1, (width * scale).roundToInt())
    val targetHeight = max(1, (height * scale).roundToInt())
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private data class CompressionAttempt(
    val bytes: ByteArray?,
    val smallestAttemptBytes: Int
)

private fun Bitmap.compressToTargetBytes(maxBytes: Int): CompressionAttempt {
    var low = MIN_JPEG_QUALITY
    var high = MAX_JPEG_QUALITY
    var bestBytes: ByteArray? = null
    var smallestAttemptBytes = Int.MAX_VALUE

    while (low <= high) {
        val quality = (low + high) / 2
        val encodedBytes = encodeJpeg(quality)
        smallestAttemptBytes = min(smallestAttemptBytes, encodedBytes.size)
        if (encodedBytes.size <= maxBytes) {
            bestBytes = encodedBytes
            low = quality + 1
        } else {
            high = quality - 1
        }
    }

    return CompressionAttempt(
        bytes = bestBytes,
        smallestAttemptBytes = smallestAttemptBytes
    )
}

private fun Bitmap.encodeJpeg(quality: Int): ByteArray {
    val outputStream = ByteArrayOutputStream()
    if (!compress(Bitmap.CompressFormat.JPEG, quality, outputStream)) {
        throw IllegalStateException("压缩图片失败")
    }
    return outputStream.toByteArray()
}

private fun Bitmap.encodePng(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    if (!compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
        throw IllegalStateException("转换图片失败")
    }
    return outputStream.toByteArray()
}

private fun String.toJpegFileName(): String {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex <= 0) {
        "$this.jpg"
    } else {
        substring(0, dotIndex) + ".jpg"
    }
}

private fun String.toPngFileName(): String {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex <= 0) {
        "$this.png"
    } else {
        substring(0, dotIndex) + ".png"
    }
}

private fun ThreadPostImage.isSupportedPostImageFormat(): Boolean {
    val normalizedMimeType = mimeType?.lowercase()
    if (normalizedMimeType in supportedPostImageMimeTypes) {
        return true
    }

    val lowerCaseFileName = fileName.lowercase()
    return lowerCaseFileName.endsWith(".jpg") ||
        lowerCaseFileName.endsWith(".jpeg") ||
        lowerCaseFileName.endsWith(".png") ||
        lowerCaseFileName.endsWith(".gif")
}

private fun ThreadPostImage.normalizeToSupportedPostImage(): ThreadPostImage {
    val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalStateException("无法解码所选图片")
    val orientedBitmap = decodedBitmap.rotateByExifOrientation(bytes)

    return if (orientedBitmap.hasAlpha()) {
        ThreadPostImage(
            fileName = fileName.toPngFileName(),
            mimeType = "image/png",
            bytes = orientedBitmap.encodePng()
        )
    } else {
        ThreadPostImage(
            fileName = fileName.toJpegFileName(),
            mimeType = "image/jpeg",
            bytes = orientedBitmap.ensureOpaqueForJpeg().encodeJpeg(MAX_JPEG_QUALITY)
        )
    }
}

private fun ThreadPostImage.isGifImage(): Boolean {
    val normalizedMimeType = mimeType?.lowercase()
    if (normalizedMimeType == "image/gif") {
        return true
    }
    return fileName.lowercase().endsWith(".gif")
}

private fun calculateCompressionScale(
    currentBytes: Int,
    targetBytes: Int
): Float {
    if (currentBytes <= targetBytes) {
        return 1f
    }

    val ratio = targetBytes.toDouble() / currentBytes.toDouble()
    val scaledRatio = sqrt(ratio).toFloat() * 0.98f
    return scaledRatio.coerceIn(0.35f, 0.92f)
}
