package com.gio.guiasclinicas.data.search

import com.gio.guiasclinicas.util.SearchFlags

/** Tipo de sección donde aparece el match. */
enum class SectionType { TEXT, TABLE, IMAGE }

/** Query normalizada + flags. */
data class SearchQuery(
    val raw: String,
    val normalized: String,
    val flags: SearchFlags
)

/** Un “hit” (una sección con una o más coincidencias). */
data class SearchHit(
    val guideSlug: String,
    val chapterSlug: String,
    val chapterPath: String,
    val sectionId: String?,
    val sectionType: SectionType,
    val matchRanges: List<IntRange>,
    val preview: String
) {
    val matchesCount: Int get() = matchRanges.size
}

/** Resultado agregado. */
data class SearchResult(
    val hits: List<SearchHit>,
    val total: Int
)

/** Estado de UI de búsqueda. */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data class Indexing(val progress: Float?, val message: String? = null) : SearchUiState
    data class Ready(val results: SearchResult, val currentHitIndex: Int) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

/** Eventos emitidos desde el indexador/buscador. */
sealed interface SearchIndexEvent {
    data class Progress(val done: Int, val total: Int) : SearchIndexEvent
    data class PartialResults(val hits: List<SearchHit>) : SearchIndexEvent
    object Done : SearchIndexEvent
}

/** Navegación (abrir capítulo en el hit). */
sealed interface SearchNavEvent {
    data class OpenHit(val hit: SearchHit) : SearchNavEvent
}
