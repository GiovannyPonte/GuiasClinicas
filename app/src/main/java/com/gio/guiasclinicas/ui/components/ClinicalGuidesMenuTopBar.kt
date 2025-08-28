package com.gio.guiasclinicas.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    showMenuIcon: Boolean,
    onMenuClick: () -> Unit,
    // 🔹 Nuevos (opcionales, compatibles hacia atrás)
    showSearchBar: Boolean = false,
    onSearch: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val listState by vm.listState.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Column {
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
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = "Seleccionar guía")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    when (val st = listState) {
                        is GuideListUiState.Loading -> {
                            DropdownMenuItem(
                                text = { Text("Cargando...") },
                                onClick = { expanded = false }
                            )
                        }
                        is GuideListUiState.Error -> {
                            DropdownMenuItem(
                                text = { Text("Error: ${st.message}") },
                                onClick = { expanded = false }
                            )
                        }
                        is GuideListUiState.Success -> {
                            st.guides.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        expanded = false
                                        onGuideSelected(g.slug)
                                        Toast.makeText(
                                            context,
                                            "Guía: ${g.title}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                        GuideListUiState.Idle -> {
                            DropdownMenuItem(
                                text = { Text("Sin datos") },
                                onClick = { expanded = false }
                            )
                        }
                    }
                }
            }
        )

        // 🔎 Barra de búsqueda opcional (solo si se solicita)
        if (showSearchBar) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {
                    onSearch(query)
                    active = false
                },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("Buscar...") },
                modifier = Modifier.fillMaxWidth()
            ) {
                // sin sugerencias por ahora
            }
        }
    }
}
