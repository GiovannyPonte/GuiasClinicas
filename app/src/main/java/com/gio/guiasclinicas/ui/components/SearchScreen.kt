package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gio.guiasclinicas.ui.state.GuideListUiState
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel

@Composable
fun SearchScreen(
    vm: GuidesViewModel,
    onGuideSelected: (String) -> Unit,
) {
    val listState by vm.listState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar guía") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        when (val st = listState) {
            is GuideListUiState.Loading -> {
                Text(text = "Cargando...")
            }
            is GuideListUiState.Error -> {
                Text(text = "Error: ${st.message}")
            }
            is GuideListUiState.Success -> {
                val guides = st.guides.filter { it.title.contains(query, ignoreCase = true) }
                LazyColumn {
                    items(guides) { guide ->
                        Text(
                            text = guide.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGuideSelected(guide.slug) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
            GuideListUiState.Idle -> Unit
        }
    }
}
