package com.gio.guiasclinicas.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp

/**
 * Basic search screen that displays a search field and a list of results.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (SearchResult) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = {
            TextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                placeholder = { Text("Buscar…") }
            )
        }, actions = {
            if (viewModel.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.clear() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
            Text(text = viewModel.results.size.toString(), modifier = Modifier.padding(end = 8.dp))
        })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.results) { result ->
                Column(
                    modifier = Modifier
                        .clickable { onResultClick(result) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "[${result.guideTitle} > ${result.chapterTitle}]",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = highlight(result.preview),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun highlight(snippet: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < snippet.length) {
        val start = snippet.indexOf('[', startIndex = i)
        val end = snippet.indexOf(']', startIndex = start + 1)
        if (start == -1 || end == -1) {
            builder.append(snippet.substring(i))
            break
        }
        if (start > i) {
            builder.append(snippet.substring(i, start))
        }
        builder.pushStyle(SpanStyle(background = Color.Yellow))
        builder.append(snippet.substring(start + 1, end))
        builder.pop()
        i = end + 1
    }
    return builder.toAnnotatedString()
}