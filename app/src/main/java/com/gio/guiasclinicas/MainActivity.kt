@file:OptIn(ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

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

    // === NUEVO: cargamos títulos de guías desde assets ===
    val context = LocalContext.current
    var guideTitles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // (slug, title)
    var menuExpanded by remember { mutableStateOf(false) }

    // Selección actual (se mantiene tu lógica)
    var selectedGuide by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }

    // Carga del JSON raíz una sola vez
    LaunchedEffect(Unit) {
        runCatching {
            val jsonText = context.assets
                .open("clinical_guidelines_db/root_manifest.json")
                .bufferedReader()
                .use { it.readText() }

            val obj = JSONObject(jsonText)
            val arr = obj.getJSONArray("guides")
            val list = (0 until arr.length()).map { i ->
                val g = arr.getJSONObject(i)
                val slug = g.getString("slug")
                val title = g.getString("title")
                slug to title
            }
            guideTitles = list
        }.onFailure {
            guideTitles = emptyList() // si falla, queda vacío
        }
    }

    // Tu lista de capítulos ficticia (no cambiamos comportamiento actual)
    val chapters = when (selectedGuide) {
        "Guías AHA de Hipertensión Arterial 2025" -> listOf("Introducción", "Diagnóstico", "Tratamiento")
        "Guías AHA de Síndrome Coronario Agudo 2025" -> listOf("Evaluación", "Manejo Inicial", "Rehabilitación")
        else -> emptyList()
    }

    LaunchedEffect(selectedGuide) {
        if (selectedGuide != null) {
            drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedGuide != null,
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
                        if (selectedGuide != null) {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "Seleccionar guía")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (guideTitles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Sin guías / error de lectura") },
                                    onClick = { menuExpanded = false }
                                )
                            } else {
                                guideTitles.forEach { (slug, title) ->
                                    DropdownMenuItem(
                                        text = { Text(title) },
                                        onClick = {
                                            selectedGuide = title   // mostramos el título en la TopBar
                                            selectedChapter = null
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favoritos") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                    )
                }
            }
        ) { innerPadding ->
            val text = runCatching {
                selectedChapter?.let { "Detalles ficticios de $it de ${selectedGuide}" }
                    ?: "Selecciona un capítulo"
            }.getOrElse { "Error al cargar la información" }
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
