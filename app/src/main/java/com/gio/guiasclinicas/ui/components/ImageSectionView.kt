package com.gio.guiasclinicas.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.res.AssetManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.ImageSection
import com.gio.guiasclinicas.ui.theme.FigureCaptionPlacement
import com.gio.guiasclinicas.ui.theme.LocalImageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ImageSectionView(section: ImageSection) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val spec = LocalImageTheme.current
    val shape = RoundedCornerShape(spec.cornerRadiusDp.dp)
    val caption = section.caption?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxWidth()) {

        // Caption como TÍTULO, si el tema lo pide
        if (caption != null && spec.captionPlacement == FigureCaptionPlacement.Top) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(spec.captionSpacingDp.dp))
        }

        // Imagen con fondo de "hueso"
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxW = maxWidth
            val targetWidthPx = with(density) { maxW.toPx().toInt() }

            val bitmap by produceState<Bitmap?>(initialValue = null, key1 = section.path, key2 = targetWidthPx) {
                val assetPath = normalizeAssetPath(section.path)
                value = withContext(Dispatchers.IO) {
                    decodeSampledBitmapFromAssets(
                        assets = ctx.assets,
                        path = assetPath,
                        targetWidthPx = max(targetWidthPx, 320)
                    )
                }
            }

            if (bitmap != null) {
                val aspect = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (aspect.isFinite() && aspect > 0f) aspect else 1f)
                        .clip(shape)
                        .background(spec.containerBg) // fondo visible tras transparencia
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = section.alt ?: caption ?: "Ilustración",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            } else {
                // Placeholder consistente con el fondo del tema
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(spec.containerBg)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Imagen no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    caption?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Caption al PIE, si el tema lo pide
        if (caption != null && spec.captionPlacement == FigureCaptionPlacement.Bottom) {
            Spacer(Modifier.height(spec.captionSpacingDp.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

// --- Helpers (igual que ya tenías) ---
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
