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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.ImageSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ImageSectionView(section: ImageSection) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val rawPath = section.path
    if (rawPath.isBlank()) return

    val assetPath = normalizeAssetPath(rawPath)

    // Usamos explícitamente el scope de BoxWithConstraints para evitar el warning.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxW: Dp = this@BoxWithConstraints.maxWidth
        val targetWidthPx = with(density) { maxW.toPx().toInt() }

        val bitmap by produceState<Bitmap?>(initialValue = null, assetPath, targetWidthPx) {
            value = withContext(Dispatchers.IO) {
                decodeSampledBitmapFromAssets(
                    assets = ctx.assets,
                    path = assetPath,
                    targetWidthPx = max(targetWidthPx, 320) // mínimo razonable
                )
            }
        }

        if (bitmap != null) {
            val aspect = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = section.alt ?: section.caption ?: "Ilustración",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(if (aspect.isFinite() && aspect > 0f) aspect else 1f),
                    contentScale = ContentScale.FillWidth
                )
                if (!section.caption.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = section.caption!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Imagen no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                section.caption?.takeIf { it.isNotBlank() }?.let {
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
}

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
