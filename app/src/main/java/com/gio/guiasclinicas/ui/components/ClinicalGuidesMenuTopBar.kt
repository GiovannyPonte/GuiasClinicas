package com.gio.guiasclinicas.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalGuidesMenuTopBar(
    vm: GuidesViewModel = viewModel(),
    onGuideSelected: (slug: String) -> Unit,
    showMenuIcon: Boolean,          // ⬅️ nuevo: controla la visibilidad del hamburguesa
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val listState by vm.listState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text("Guías Clínicas") },
        navigationIcon = {
            // Solo mostramos el hamburguesa si ya hay una guía seleccionada/cargada
            if (showMenuIcon) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                }
            }
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Outlined.MenuBook, contentDescription = "Seleccionar guía")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                when (listState) {
                    is GuideListUiState.Loading -> DropdownMenuItem(
                        text = { Text("Cargando...") },
                        onClick = { expanded = false }
                    )
                    is GuideListUiState.Error -> {
                        val msg = (listState as GuideListUiState.Error).message
                        DropdownMenuItem(
                            text = { Text("Error: $msg") },
                            onClick = { expanded = false }
                        )
                    }
                    is GuideListUiState.Success -> {
                        val guides = (listState as GuideListUiState.Success).guides
                        guides.forEach { g ->
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
            }
        }
    )
}
