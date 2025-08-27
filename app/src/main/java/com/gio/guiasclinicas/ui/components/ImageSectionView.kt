package com.gio.guiasclinicas.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.res.AssetManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.ImageSection
import com.gio.guiasclinicas.ui.components.image.ImageMemoryCache
import com.gio.guiasclinicas.ui.components.zoom.ZoomableImageContainer
import com.gio.guiasclinicas.ui.theme.FigureCaptionPlacement
import com.gio.guiasclinicas.ui.theme.LocalImageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ImageSectionView(
    section: ImageSection,
    captionText: AnnotatedString? = null,
    footnoteText: AnnotatedString? = null
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val spec = LocalImageTheme.current
    val caption = captionText ?: section.caption?.takeIf { it.isNotBlank() }?.let { AnnotatedString(it) }
    val assetPath = normalizeAssetPath(section.path)

    Column(Modifier.fillMaxWidth()) {
        if (caption != null && spec.captionPlacement == FigureCaptionPlacement.Top) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(spec.captionSpacingDp.dp))
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val maxW = maxWidth
            val targetWidthPx = with(density) { maxW.toPx().toInt() }
            val key = ImageMemoryCache.makeKey(assetPath, targetWidthPx)

            var bitmap by remember(key) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(key) {
                // intenta caché primero
                val cached = ImageMemoryCache.get(key)
                if (cached != null) {
                    bitmap = cached
                } else {
                    // decodifica a tamaño objetivo (IO)
                    val bmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmapFromAssets(ctx.assets, assetPath, max(targetWidthPx, 320))
                    }
                    if (bmp != null) {
                        ImageMemoryCache.put(key, bmp)
                        bitmap = bmp
                    }
                }
            }

            bitmap?.let {
                ZoomableImageContainer(
                    bitmap = it.asImageBitmap(),
                    contentDescription = section.alt ?: caption?.text ?: "Ilustración",
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Imagen no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (caption != null && spec.captionPlacement == FigureCaptionPlacement.Bottom) {
            Spacer(Modifier.height(spec.captionSpacingDp.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }

        footnoteText?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

// Helpers
private fun decodeSampledBitmapFromAssets(
    assets: AssetManager,
    path: String,
    targetWidthPx: Int
): Bitmap? {
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    assets.open(path).use { BitmapFactory.decodeStream(it, null, optsBounds) }
    if (optsBounds.outWidth <= 0 || optsBounds.outHeight <= 0) return null

    var inSample = 1
    while ((optsBounds.outWidth / inSample) > targetWidthPx) inSample *= 2

    val opts = BitmapFactory.Options().apply {
        inSampleSize = inSample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun normalizeAssetPath(raw: String): String {
    val s1 = raw.replace('\\', '/')
    return s1.substringAfter("src/main/assets/", s1)
        .substringAfter("assets/", s1)
        .trimStart('/')
}
