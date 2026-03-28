package ink.duo3.fogisland.ui.components.imageviewer

import android.graphics.Bitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect

data class ImageViewerPreviewState(
    val image: String,
    val ext: String?,
    val bitmap: Bitmap,
    val boundsInRoot: Rect,
    val alignment: Alignment,
    val cornerRadiusPx: Float
)
