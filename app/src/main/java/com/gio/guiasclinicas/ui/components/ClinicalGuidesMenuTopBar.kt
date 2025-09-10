package com.gio.guiasclinicas.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.R
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalGuidesMenuTopBar(
    vm: GuidesViewModel = viewModel(),
    onGuideSelected: (slug: String) -> Unit,
    showMenuIcon: Boolean,
    onMenuClick: () -> Unit,
    title: String,
    showSearchBar: Boolean = false,
    onSearch: (String) -> Unit = {},
    // Opcionales para mejorar UX sin romper llamadas actuales:
    selectedSlug: String? = null,        // marca la guía activa con ✔
    useDictionaryIcon: Boolean = false   // usa ic_dictionary_24 si lo tienes
) {
    val context = LocalContext.current
    val listState by vm.listState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Column {
        val scroll = TopAppBarDefaults.pinnedScrollBehavior()

        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = cs.primaryContainer,
                scrolledContainerColor = cs.primary,
                titleContentColor = cs.onPrimaryContainer,
                navigationIconContentColor = cs.onPrimaryContainer,
                actionIconContentColor = cs.onPrimaryContainer
            ),
            scrollBehavior = scroll,
            title = { AutoResizeTitleText(text = title, maxLines = 2) },
            navigationIcon = {
                if (showMenuIcon) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                }
            },
            actions = {
                IconButton(onClick = { expanded = true }) {
                    if (useDictionaryIcon) {
                        Icon(
                            painterResource(R.drawable.ic_dictionary_24),
                            contentDescription = "Seleccionar guía"
                        )
                    } else {
                        Icon(Icons.Outlined.MenuBook, contentDescription = "Seleccionar guía")
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .widthIn(min = 300.dp)
                        .shadow(12.dp, shape)
                        .background(cs.surface, shape)
                        .border(1.dp, cs.outline.copy(alpha = 0.25f), shape)
                ) {
                    Column(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                        Text(
                            "Guías disponibles",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        HorizontalDivider()

                        when (val st = listState) {
                            is GuideListUiState.Loading -> {
                                DropdownMenuItem(
                                    text = { Text("Cargando…", style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { expanded = false },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            is GuideListUiState.Error -> {
                                DropdownMenuItem(
                                    text = { Text("Error: ${st.message}", style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { expanded = false },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            is GuideListUiState.Success -> {
                                st.guides.forEachIndexed { index, g ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            if (useDictionaryIcon) {
                                                Icon(
                                                    painterResource(R.drawable.ic_dictionary_24),
                                                    contentDescription = null
                                                )
                                            } else {
                                                Icon(Icons.Outlined.MenuBook, null)
                                            }
                                        },
                                        trailingIcon = {
                                            if (selectedSlug != null && g.slug == selectedSlug) {
                                                Icon(Icons.Outlined.Check, contentDescription = null)
                                            }
                                        },
                                        text = {
                                            Text(
                                                g.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            expanded = false
                                            onGuideSelected(g.slug)
                                            Toast.makeText(
                                                context,
                                                "Guía: ${g.title}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                    if (index != st.guides.lastIndex) {
                                        HorizontalDivider(color = cs.outline.copy(alpha = 0.15f))
                                    }
                                }
                            }
                            GuideListUiState.Idle -> {
                                DropdownMenuItem(
                                    text = { Text("Sin datos", style = MaterialTheme.typography.bodyLarge) },
                                    onClick = { expanded = false },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        )

        if (showSearchBar) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {
                    onSearch(query); active = false
                },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("Buscar…") },
                modifier = Modifier.fillMaxWidth()
            ) { /* sugerencias opcionales */ }
        }
    }
}
