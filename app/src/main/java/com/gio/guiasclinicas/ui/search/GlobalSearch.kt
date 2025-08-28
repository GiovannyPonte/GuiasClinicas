package com.gio.guiasclinicas.ui.search

import com.gio.guiasclinicas.data.repo.GuidesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a search match across all guides and chapters.
 * [result] holds the original section match (keeps chapter-local index).
 */
data class ScopedSearchResult(
    val guideSlug: String,
    val guideTitle: String,
    val guideDir: String,
    val chapterPath: String,
    val chapterTitle: String,
    val result: SearchResult
)

/**
 * Searches [query] in every guide/chapter available through [repo].
 * - Keeps SearchResult.index as chapter-local index (no global override).
 * - Resolves chapterPath robustly (manifestPath / path / chapterPath / file).
 */
suspend fun searchAllGuides(
    repo: GuidesRepository,
    query: String,
    ignoreCase: Boolean = true,
    ignoreAccents: Boolean = true
): List<ScopedSearchResult> = withContext(Dispatchers.IO) {
    if (query.isBlank()) return@withContext emptyList()

    val root = repo.loadRootManifest()
    val matches = mutableListOf<ScopedSearchResult>()

    root.guides.forEach { guide ->
        runCatching {
            val manifest = repo.loadGuideManifestByPath(guide.manifestPath)
            val dir = repo.guideDirFromManifestPath(guide.manifestPath)

            manifest.chapters.forEach { chapter ->
                val chapterPath = chapterPathOf(chapter) ?: return@forEach

                val content = repo.loadChapterContent(dir, chapterPath)
                val sections = content.content.sections

                val sectionMatches = searchSections(sections, query, ignoreCase, ignoreAccents)
                sectionMatches.forEach { res ->
                    // NO cambiamos res.index: sigue siendo el índice local del capítulo.
                    matches.add(
                        ScopedSearchResult(
                            guideSlug = guide.slug,
                            guideTitle = manifest.guide.title,
                            guideDir = dir,
                            chapterPath = chapterPath,
                            chapterTitle = chapter.title,
                            result = res
                        )
                    )
                }
            }
        }
        // Si una guía falla, continuamos con las demás.
    }
    matches
}

/**
 * Intenta extraer una ruta válida del capítulo probando campos comunes.
 * Compatible con variantes de modelos sin romper el tipado.
 */
private fun chapterPathOf(chapter: Any): String? {
    val candidates = listOf("manifestPath", "path", "chapterPath", "file")
    for (name in candidates) {
        runCatching {
            val f = chapter.javaClass.getDeclaredField(name)
            f.isAccessible = true
            val v = f.get(chapter) as? String
            if (!v.isNullOrBlank()) return v
        }
    }
    return null
}
