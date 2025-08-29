package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Simple search bar used inside a chapter view. The UI is intentionally
 * lightweight – the component exposes callbacks so the owning screen can
 * drive the search behaviour (next/prev navigation, history management,
 * case/accents toggles, etc.).
 */
@Composable
fun ChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {},
    onClose: () -> Unit = {},
    ignoreCase: Boolean = true,
    onToggleCase: (Boolean) -> Unit = {},
    ignoreAccents: Boolean = true,
    onToggleAccents: (Boolean) -> Unit = {},
    history: List<String> = emptyList(),
    onHistorySelected: (String) -> Unit = {},
    onRemoveHistory: (String) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = onQueryChange
            )
            // Navigate through matches
            IconButton(onClick = onPrev) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Siguiente")
            }
            // Close search mode
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
            }
        }
        // Case and accent toggles
        Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Text(
                text = if (ignoreCase) "Aa" else "aA",
                modifier = Modifier
                    .clickable { onToggleCase(!ignoreCase) }
                    .padding(end = 16.dp)
            )
            Text(
                text = if (ignoreAccents) "áA" else "aA",
                modifier = Modifier.clickable { onToggleAccents(!ignoreAccents) }
            )
        }
        // Search history list
        if (history.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(history) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHistorySelected(item) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveHistory(item) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Eliminar del historial")
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Limpiar historial",
                            modifier = Modifier.clickable { onClearHistory() }
                        )
                    }
                }
            }
        }
    }
}
