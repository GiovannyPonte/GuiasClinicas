package com.gio.guiasclinicas.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CaseSensitive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.data.model.GuideRef
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@Composable
fun SearchScreen(viewModel: GuidesViewModel = viewModel()) {
    val listState by viewModel.listState.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var caseSensitive by rememberSaveable { mutableStateOf(false) }

    val guides: List<GuideRef> = when (val state = listState) {
        is GuideListUiState.Success -> state.guides
        else -> emptyList()
    }

    val filtered = remember(query, caseSensitive, guides) {
        val q = if (caseSensitive) query else query.lowercase()
        guides.filter { guide ->
            val title = if (caseSensitive) guide.title else guide.title.lowercase()
            title.contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar guía") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { caseSensitive = !caseSensitive }) {
                    Icon(
                        imageVector = Icons.Outlined.CaseSensitive,
                        contentDescription = if (caseSensitive) {
                            "Búsqueda sensible a mayúsculas activada"
                        } else {
                            "Búsqueda sensible a mayúsculas desactivada"
                        }
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { guide ->
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}