package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.favorites.FavoriteChapter

@Composable
fun FavoritesSheet(
    items: List<FavoriteChapter>,
    onOpen: (FavoriteChapter) -> Unit,
    onRemoveOne: (FavoriteChapter) -> Unit,
    onRemoveAll: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Favoritos", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text("Aún no has agregado capítulos a favoritos.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { fav ->
                    ElevatedCard(onClick = { onOpen(fav) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(fav.chapterTitle, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(2.dp))
                                Text(fav.guideTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onRemoveOne(fav) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Quitar")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { confirmClear = true }) { Text("Borrar todos") }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Borrar todos los favoritos") },
            text = { Text("¿Seguro que deseas borrar toda la lista de favoritos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onRemoveAll() }) {
                    Text("Sí, borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar") }
            }
        )
    }
}
