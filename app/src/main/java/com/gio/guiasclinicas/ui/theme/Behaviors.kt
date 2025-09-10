@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.gio.guiasclinicas.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/** Eleva un contenedor cuando está pressed */
@Composable
fun Modifier.pressElevation(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { inter ->
            pressed = when (inter) {
                is PressInteraction.Press -> true
                is PressInteraction.Release, is PressInteraction.Cancel -> false
                else -> pressed
            }
        }
    }
    val elev by animateDpAsState(if (pressed) AppElevation.md else AppElevation.sm, label = "pressElev")
    return this.shadow(elev, MaterialTheme.shapes.medium, clip = false)
}

/** AppBar con color de contenedor y sombra en scroll */
@Composable
fun modernTopAppBarColors() = TopAppBarDefaults.centerAlignedTopAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
)
