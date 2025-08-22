@file:Suppress("BoxWithConstraintsScope")

package com.gio.guiasclinicas.ui.components.zoom
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.ui.theme.LocalImageTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Comportamiento:
 * - A 1× la imagen SIEMPRE se ve completa (ContentScale.Fit + baseScale si hace falta).
 * - Pan solo cuando effectiveScale > 1.
 * - Al rotar, se resetea el zoom y se recentra.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImageContainer(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 4f
) {
    val theme = LocalImageTheme.current
    val shape = RoundedCornerShape(theme.cornerRadiusDp.dp)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Estado de zoom/pan del usuario
    var userScale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset por rotación
    LaunchedEffect(configuration.orientation) {
        userScale = 1f
        offset = Offset.Zero
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidthDp = maxWidth
        val containerWidthPx = with(density) { containerWidthDp.toPx() }

        val imgAspect = bitmap.width.toFloat() / max(1, bitmap.height).toFloat()

        // Altura ideal para llenar el ancho respetando el aspecto
        val idealHeightPx = containerWidthPx / imgAspect

        // Limitar altura para que la imagen no monopolice la pantalla
        val screenHdp = configuration.screenHeightDp.dp
        val maxHFraction =
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 0.72f else 0.95f
        val maxHeightPx = with(density) { (screenHdp * maxHFraction).toPx() }

        // Si la altura ideal no cabe, reducimos solo el CONTENEDOR (no la escala del Image)
        val baseScale = if (idealHeightPx <= maxHeightPx) 1f else (maxHeightPx / idealHeightPx)

        // Altura final del contenedor
        val containerHeightPx = if (baseScale < 1f) maxHeightPx else idealHeightPx
        val containerHeight = with(density) { containerHeightPx.toDp() }

        /**
         * Con ContentScale.Fit, el contenido ajustado (a 1x) ocupa:
         *   fitW = min(containerW, containerH * aspect)
         *   fitH = min(containerH, containerW / aspect)
         * Usamos ese tamaño para limitar el pan correctamente.
         */
        fun contentFitSizeAt1x(): Pair<Float, Float> {
            val fitW = min(containerWidthPx, containerHeightPx * imgAspect)
            val fitH = min(containerHeightPx, containerWidthPx / imgAspect)
            return fitW to fitH
        }

        fun clampOffsets(scale: Float, x: Float, y: Float): Offset {
            val (contentW, contentH) = contentFitSizeAt1x()
            val scaledW = contentW * scale
            val scaledH = contentH * scale
            val maxX = max(0f, (scaledW - containerWidthPx) / 2f)
            val maxY = max(0f, (scaledH - containerHeightPx) / 2f)
            return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
        }

        // Transformaciones: la imagen ya está "encajada" con Fit, el zoom es SOLO userScale
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newUser = (userScale * zoomChange).coerceIn(minScale, maxScale)
            userScale = newUser
            offset = if (newUser > 1f) {
                clampOffsets(newUser, offset.x + panChange.x, offset.y + panChange.y)
            } else {
                Offset.Zero
            }
        }

        val effectiveScale = userScale // <- ya NO multiplicamos por baseScale

        Box(
            modifier = Modifier
                .width(containerWidthDp)
                .height(containerHeight)
                .clip(shape)
                .background(theme.containerBg)
                .transformable(
                    state = transformState,
                    canPan = { userScale > 1f } // pan solo si hay zoom del usuario
                )
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Escala aplicada únicamente por el usuario
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                val target = if (userScale < 1.9f) min(2f, maxScale) else 1f
                                userScale = target
                                offset = clampOffsets(target, offset.x, offset.y)
                            }
                        )
                    },
                // Nunca recorta: la imagen se contiene dentro del contenedor
                contentScale = ContentScale.Fit
            )

            // + / - discretos
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val t = (userScale * 1.25f).coerceIn(minScale, maxScale)
                        userScale = t
                        offset = clampOffsets(t, offset.x, offset.y)
                    },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    )
                ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) }

                FilledTonalIconButton(
                    onClick = {
                        val t = (userScale / 1.25f).coerceAtLeast(minScale)
                        userScale = t
                        offset = clampOffsets(t, offset.x, offset.y)
                    },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    )
                ) { Icon(Icons.Filled.Remove, null, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
