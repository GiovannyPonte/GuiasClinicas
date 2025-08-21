package com.gio.guiasclinicas.ui.components.zoom

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Velocity


/** Sencillo bus para notificar "reseteen a 1x". */
class ZoomResetDispatcher {
    private val listeners = mutableSetOf<suspend () -> Unit>()
    fun register(l: suspend () -> Unit) { listeners += l }
    fun unregister(l: suspend () -> Unit) { listeners -= l }
    suspend fun resetAll() { for (l in listeners) l() }
}

val LocalZoomResetDispatcher = staticCompositionLocalOf { ZoomResetDispatcher() }

/** Proveedor del bus (ponlo a nivel de pantalla). */
@Composable
fun ZoomResetHost(content: @Composable () -> Unit) {
    val dispatcher = remember { ZoomResetDispatcher() }
    CompositionLocalProvider(LocalZoomResetDispatcher provides dispatcher) {
        content()
    }
}

/**
 * Úsalo en el contenedor con verticalScroll (padre).
 * Al iniciar scroll vertical disparamos reset de las imágenes ANTES de desplazar.
 */
fun Modifier.resetZoomOnParentVerticalScroll(scope: CoroutineScope): Modifier = composed {
    val dispatcher = LocalZoomResetDispatcher.current
    var didResetThisDrag by remember { mutableStateOf(false) }

    val connection = remember {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Solo nos interesa un gesto vertical de tipo "Drag" (dedo/mouse arrastrando)
                if (source == NestedScrollSource.Drag &&
                    kotlin.math.abs(available.y) > kotlin.math.abs(available.x)
                ) {
                    // Resetea UNA sola vez al inicio del gesto
                    if (!didResetThisDrag) {
                        didResetThisDrag = true
                        scope.launch { dispatcher.resetAll() }
                    }
                    // *** CLAVE: no consumir el delta. Deja pasar el scroll a la LazyColumn.
                    return Offset.Zero
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Fin del gesto: prepara para un futuro reset
                didResetThisDrag = false
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                didResetThisDrag = false
                return Velocity.Zero
            }
        }
    }
    this.nestedScroll(connection)
}