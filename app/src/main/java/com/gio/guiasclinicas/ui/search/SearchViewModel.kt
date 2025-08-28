package com.gio.guiasclinicas.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel that exposes the search query and results applying a debounce to
 * limit the number of database lookups.
 */
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    var query by mutableStateOf("")
        private set

    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            results = repository.search(query)
        }
    }

    fun clear() {
        onQueryChange("")
    }
}