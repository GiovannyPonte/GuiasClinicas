package com.gio.guiasclinicas.data.search

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.util.SearchFlags
import com.gio.guiasclinicas.util.normalizeForSearch




private const val TAG_UI = "SearchUI"

@Composable
fun SearchScreen(
    ui: SearchUiState,
    // 🔽 NUEVO: historial expuesto desde la VM
    recentQueries: List<String>,
    onPickRecent: (String) -> Unit,
    onClearHistory: () -> Unit,
    // existentes
    onQuery: (String, SearchFlags) -> Unit,
    onOpenHit: (SearchHit) -> Unit,
    onExit: () -> Unit
) {
    Log.d(TAG_UI, "Compose SearchScreen() - state=${ui::class.simpleName}")

    // Inputs
    var text by remember { mutableStateOf("") }
    var case by remember { mutableStateOf(false) }
    var accent by remember { mutableStateOf(false) }

    // Droplist de historial
    var historyOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        // ===== Barra de búsqueda =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Usamos un Box para anclar el DropdownMenu al TextField
            Box(modifier = Modifier.weight(1f)) {
                // 1) OutlinedTextField estable (sin onValueChange lateral)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar") },
                    leadingIcon = {
                        IconButton(onClick = { historyOpen = !historyOpen }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Historial de búsqueda")
                        }
                    },
                    trailingIcon = {
                        if (text.isNotBlank()) {
                            IconButton(onClick = { text = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Limpiar")
                            }
                        }
                    }
                )

                // 2) Reserva de 1dp invisible para evitar “saltos” visuales del borde inferior
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomStart)
                )

                // 3) Menú popup: no participa del layout -> no cambia el alto del Row
                DropdownMenu(
                    expanded = historyOpen,
                    onDismissRequest = { historyOpen = false },
                    properties = androidx.compose.ui.window.PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        clippingEnabled = true
                    ),
                    // Despega 6dp bajo el TextField para que no “pegue” al borde ni lo relayout
                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 6.dp),
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .heightIn(max = 280.dp)
                ) {
                    if (recentQueries.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin historial", style = MaterialTheme.typography.bodySmall) },
                            onClick = { historyOpen = false }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            recentQueries.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        text = q
                                        onPickRecent(q)
                                        historyOpen = false
                                    }
                                )
                            }
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Limpiar historial", style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onClearHistory()
                                historyOpen = false
                            }
                        )
                    }
                }
            }





            Button(
                onClick = {
                    val flags = SearchFlags(caseSensitive = case, accentSensitive = accent)
                    val norm = text.normalizeForSearch(flags).normalized
                    Log.d(TAG_UI, "BUSCAR -> raw='$text', norm='$norm', flags=$flags")
                    onQuery(text, flags)
                },
                enabled = text.isNotBlank()
            ) { Text("Buscar") }
        }

        // ===== Filtros (chips) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = case,
                    onClick = { case = !case },
                    label = { Text("Distinguir mayúsculas", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = accent,
                    onClick = { accent = !accent },
                    label = { Text("Distinguir acentos", style = MaterialTheme.typography.labelMedium) }
                )
            }
            TextButton(onClick = onExit) { Text("Cerrar") }
        }

        // ===== Resultados =====
        when (ui) {
            is SearchUiState.Idle -> Box(Modifier.fillMaxSize())

            is SearchUiState.Indexing -> {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                    ui.message?.let { Text(it, modifier = Modifier.padding(horizontal = 12.dp)) }
                }
            }

            is SearchUiState.Error ->
                Text(
                    "Error: ${ui.message}",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error
                )

            is SearchUiState.Ready -> {
                val result = ui.results
                if (result.total == 0) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "No se encontraron coincidencias.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.hits) { hit ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenHit(hit) }
                            ) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "${hit.guideSlug} > ${hit.chapterSlug} · ${hit.sectionType}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        AnnotatedString(hit.preview),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${hit.matchesCount} coincidencia(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
