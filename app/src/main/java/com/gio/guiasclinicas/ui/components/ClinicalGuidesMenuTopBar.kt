package com.gio.guiasclinicas.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalGuidesMenuTopBar(
    vm: GuidesViewModel,
    onGuideSelected: (slug: String) -> Unit,
    showMenuIcon: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    // favoritos
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val listState by vm.listState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text("Guías Clínicas") },
        navigationIcon = {
            if (showMenuIcon) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                }
            }
        },
        actions = {
            // Buscar
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar")
            }
            // Estrella favoritos
            IconButton(onClick = onToggleFavorite) {
                if (isFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Quitar de favoritos")
                } else {
                    Icon(Icons.Outlined.StarBorder, contentDescription = "Agregar a favoritos")
                }
            }
            // Selector de guía
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Outlined.MenuBook, contentDescription = "Seleccionar guía")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                when (val st = listState) {
                    is GuideListUiState.Loading ->
                        DropdownMenuItem(text = { Text("Cargando...") }, onClick = { expanded = false })
                    is GuideListUiState.Error ->
                        DropdownMenuItem(text = { Text("Error: ${st.message}") }, onClick = { expanded = false })
                    is GuideListUiState.Success -> {
                        val items = st.guides
                        if (items.isEmpty()) {
                            DropdownMenuItem(text = { Text("Sin datos") }, onClick = { expanded = false })
                        } else {
                            items.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        expanded = false
                                        onGuideSelected(g.slug)
                                        Toast.makeText(context, "Guía: ${g.title}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                    GuideListUiState.Idle ->
                        DropdownMenuItem(text = { Text("Sin datos") }, onClick = { expanded = false })
                }
            }
        }
    )
}
