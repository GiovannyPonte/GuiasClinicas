package com.gio.guiasclinicas.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CaseSensitiveAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.util.normalizeText

@Composable
fun SearchScreen(onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var accentSensitive by remember { mutableStateOf(true) }
    var historyExpanded by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    val normalizedQuery = remember(query, caseSensitive, accentSensitive) {
        normalizeText(query, caseSensitive, accentSensitive)
    }

    BackHandler(onBack = onClose)

    Column(modifier = Modifier.padding(16.dp)) {
        Box {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = { historyExpanded = !historyExpanded }) {
                        Icon(
                            imageVector = if (historyExpanded) Icons.Filled.HistoryToggleOff else Icons.Filled.History,
                            contentDescription = "Historial"
                        )
                    }
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Limpiar")
                        }
                        IconToggleButton(checked = caseSensitive, onCheckedChange = { caseSensitive = it }) {
                            Icon(imageVector = Icons.Filled.CaseSensitiveAlt, contentDescription = "Mayúsculas")
                        }
                        IconToggleButton(checked = accentSensitive, onCheckedChange = { accentSensitive = it }) {
                            Icon(imageVector = Icons.Filled.Translate, contentDescription = "Acentos")
                        }
                    }
                },
                placeholder = { Text("Buscar") }
            )
            DropdownMenu(
                expanded = historyExpanded && history.isNotEmpty(),
                onDismissRequest = { historyExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                history.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            query = item
                            historyExpanded = false
                        }
                    )
                }
            }
        }

        if (query.isNotBlank()) {
            Text(text = normalizedQuery, modifier = Modifier.padding(top = 8.dp))
        }
    }
}