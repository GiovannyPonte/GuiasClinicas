package com.gio.guiasclinicas.ui.search

import com.gio.guiasclinicas.data.repo.GuidesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a search match across all guides and chapters.
 * [result] holds the original section match with global index.
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
                val content = repo.loadChapterContent(dir, chapter.manifestPath)
                val sections = content.content.sections
                val sectionMatches = searchSections(sections, query, ignoreCase, ignoreAccents)
                sectionMatches.forEach { res ->
                    val globalIndex = matches.size
                    matches.add(
                        ScopedSearchResult(
                            guideSlug = guide.slug,
                            guideTitle = manifest.guide.title,
                            guideDir = dir,
                            chapterPath = chapter.manifestPath,
                            chapterTitle = chapter.title,
                            result = res.copy(index = globalIndex)
                        )
                    )
                }
            }
        }
    }
    matches
}