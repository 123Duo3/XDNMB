package ink.duo3.fogisland.ui.components.imageviewer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import coil.compose.AsyncImagePainter

@Composable
internal fun ImageViewerCanvas(
    layout: ImageViewerLayout,
    displayedScale: Float,
    displayedTranslation: Offset,
    imageBitmap: ImageBitmap?,
    painter: AsyncImagePainter,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        withTransform({
            translate(
                left = displayedTranslation.x,
                top = displayedTranslation.y
            )
            scale(
                scaleX = displayedScale,
                scaleY = displayedScale,
                pivot = Offset.Zero
            )
        }) {
            imageBitmap?.let { bitmap ->
                drawImage(image = bitmap)
            } ?: with(painter) {
                draw(
                    size = Size(
                        width = layout.baseSize.width,
                        height = layout.baseSize.height
                    )
                )
            }
        }
    }
}
