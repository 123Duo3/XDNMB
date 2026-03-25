package ink.duo3.fogisland.ui.components

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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_POST_IMAGE_BYTES = 5 * 1024 * 1024
private const val MAX_JPEG_QUALITY = 95
private const val MIN_JPEG_QUALITY = 35
private val compressionScaleSteps = listOf(1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f)
private val supportedPostImageMimeTypes = setOf("image/jpeg", "image/png")

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
    isCompressingImage: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onCompressImage: () -> Unit,
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
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onPickImage) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text("选择")
                    }
                    if (selectedImage != null) {
                        IconButton(onClick = onRemoveImage) {
                            Icon(Icons.Default.Close, contentDescription = "移除图片")
                        }
                    }
                }
            }

            if (selectedImage != null) {
                if (isImageTooLarge) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "图片过大，可能发送失败",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "如有需要，可先压缩后再发送。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onCompressImage,
                            enabled = !isCompressingImage
                        ) {
                            Text(if (isCompressingImage) "压缩中…" else "压缩")
                        }
                    }
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

        val image = ThreadPostImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )

        if (image.isSupportedPostImageFormat()) {
            image
        } else {
            image.normalizeToSupportedPostImage()
        }
    }
}

suspend fun Context.compressPostImage(image: ThreadPostImage): ThreadPostImage {
    return withContext(Dispatchers.Default) {
        if (image.bytes.size <= MAX_POST_IMAGE_BYTES) {
            return@withContext image
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
            ?: throw IllegalStateException("无法解码所选图片")
        val orientedBitmap = decodedBitmap.rotateByExifOrientation(image.bytes)
        val jpegReadyBitmap = orientedBitmap.ensureOpaqueForJpeg()

        for (scale in compressionScaleSteps) {
            val candidateBitmap = jpegReadyBitmap.scaledForCompression(scale)
            val compressedBytes = candidateBitmap.compressToTargetBytes(MAX_POST_IMAGE_BYTES)
            if (candidateBitmap !== jpegReadyBitmap) {
                candidateBitmap.recycle()
            }
            if (compressedBytes != null) {
                return@withContext ThreadPostImage(
                    fileName = image.fileName.toJpegFileName(),
                    mimeType = "image/jpeg",
                    bytes = compressedBytes
                )
            }
        }

        throw IllegalStateException("无法将图片压缩到可发送大小")
    }
}

fun isPostImageTooLarge(image: ThreadPostImage?): Boolean {
    return image?.bytes?.size?.let { it > MAX_POST_IMAGE_BYTES } == true
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

private fun Bitmap.compressToTargetBytes(maxBytes: Int): ByteArray? {
    var low = MIN_JPEG_QUALITY
    var high = MAX_JPEG_QUALITY
    var bestBytes: ByteArray? = null

    while (low <= high) {
        val quality = (low + high) / 2
        val encodedBytes = encodeJpeg(quality)
        if (encodedBytes.size <= maxBytes) {
            bestBytes = encodedBytes
            low = quality + 1
        } else {
            high = quality - 1
        }
    }

    return bestBytes
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
        lowerCaseFileName.endsWith(".png")
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
