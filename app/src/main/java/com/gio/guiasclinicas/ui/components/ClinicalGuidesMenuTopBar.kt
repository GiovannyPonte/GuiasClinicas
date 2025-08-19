package com.gio.guiasclinicas.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalGuidesMenuTopBar(
    vm: GuidesViewModel = viewModel(),
    onGuideSelected: (slug: String) -> Unit
) {
    val context = LocalContext.current
    val listState by vm.listState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Guías Clínicas") },
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
                        DropdownMenuItem(text = { Text("Error: $msg") }, onClick = { expanded = false })
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
