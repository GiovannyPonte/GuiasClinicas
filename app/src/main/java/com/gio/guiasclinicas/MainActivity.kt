@file:OptIn(ExperimentalMaterial3Api::class)
package com.gio.guiasclinicas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import androidx.compose.material3.ExperimentalMaterial3Api


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuiasClinicasTheme {
                GuidesApp()
            }
        }
    }
}

@Composable
fun GuidesApp() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedGuide by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val chapters = when (selectedGuide) {
        "HTA 2025" -> listOf("Introducción", "Diagnóstico", "Tratamiento")
        "SCA 2025" -> listOf("Evaluación", "Manejo Inicial", "Rehabilitación")
        else -> emptyList()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = selectedGuide ?: "",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                chapters.forEach { chapter ->
                    NavigationDrawerItem(
                        label = { Text(chapter) },
                        selected = chapter == selectedChapter,
                        onClick = {
                            selectedChapter = chapter
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedGuide ?: "Guías Clínicas") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Seleccionar guía")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("HTA 2025") },
                                onClick = {
                                    selectedGuide = "HTA 2025"
                                    selectedChapter = null
                                    menuExpanded = false
                                    scope.launch { drawerState.open() }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("SCA 2025") },
                                onClick = {
                                    selectedGuide = "SCA 2025"
                                    selectedChapter = null
                                    menuExpanded = false
                                    scope.launch { drawerState.open() }
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                    )
                }
            }
        ) { innerPadding ->
            val text = selectedChapter?.let { "Detalles ficticios de $it de ${selectedGuide}" }
                ?: "Selecciona un capítulo"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuidesAppPreview() {
    GuiasClinicasTheme {
        GuidesApp()
    }
}
