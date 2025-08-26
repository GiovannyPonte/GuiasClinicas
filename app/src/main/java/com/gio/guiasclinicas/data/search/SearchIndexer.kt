package com.gio.guiasclinicas.data.search

import android.util.Log
import com.gio.guiasclinicas.data.model.ChapterContent
import com.gio.guiasclinicas.data.model.ChapterEntry
import com.gio.guiasclinicas.data.model.ImageSection
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.data.model.TextSection
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.util.NormalizedText
import com.gio.guiasclinicas.util.SearchFlags
import com.gio.guiasclinicas.util.buildPreview
import com.gio.guiasclinicas.util.findAllMatches
import com.gio.guiasclinicas.util.normalizeForSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

private const val TAG_INDEXER = "SEARCH_INDEXER"

/**
 * Indexador de capítulos con índice compacto por bloque (texto / tabla / imagen).
 * - Normaliza respetando flags (case/acentos).
 * - Devuelve ranges SIEMPRE en índices del texto ORIGINAL (usando NormalizedText.indexMap).
 */
class SearchIndexer(
    private val repo: GuidesRepository,
    private val maxChaptersInCache: Int = 8
) {

    // ---------- Tipos internos ----------
    private data class BlockIndex(
        val sectionId: String?,
        val sectionType: SectionType,
        val originalText: String,
        val normalized: NormalizedText
    )

    private data class ChapterIndex(
        val chapterSlug: String,
        val chapterPath: String,
        val blocks: List<BlockIndex>
    )

    // LRU por capítulo (clave = "$guideDir/$chapterPath")
    private val chapterCache = object : LinkedHashMap<String, ChapterIndex>(maxChaptersInCache, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ChapterIndex>?): Boolean {
            return size > maxChaptersInCache
        }
    }

    // ============================================================
    //  OVERLOAD #1: compat con VM (dir, slug, query, scope)
    // ============================================================
    fun searchInGuide(
        guideDir: String,
        guideSlug: String,
        query: SearchQuery,
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
    ): Flow<SearchIndexEvent> = flow {
        Log.d(TAG_INDEXER, "searchInGuide(4-args): dir=$guideDir, slug=$guideSlug, raw='${query.raw}'")
        val manifest = withContext(Dispatchers.IO) {
            repo.loadGuideManifestByPath("$guideDir/manifest.json")
        }
        emitAllInternal(guideDir, guideSlug, manifest.chapters, query)
    }

    // ============================================================
    //  OVERLOAD #2: compat con VM (chapters ya cargados)
    // ============================================================
    fun searchInGuide(
        guideDir: String,
        guideSlug: String,
        chapters: List<ChapterEntry>,
        query: SearchQuery
    ): Flow<SearchIndexEvent> = flow {
        Log.d(TAG_INDEXER, "searchInGuide(chapters): dir=$guideDir, slug=$guideSlug, count=${chapters.size}")
        emitAllInternal(guideDir, guideSlug, chapters, query)
    }

    // ============================================================
    //  BÚSQUEDA GLOBAL EN TODAS LAS GUÍAS
    // ============================================================
    fun searchEverywhere(query: SearchQuery): Flow<SearchIndexEvent> = flow {
        Log.d(TAG_INDEXER, "searchEverywhere(): raw='${query.raw}' flags=${query.flags}")

        val root = withContext(Dispatchers.IO) { repo.loadRootManifest() }
        val totalChapters = withContext(Dispatchers.IO) {
            root.guides.sumOf { gref ->
                repo.loadGuideManifestByPath(gref.manifestPath).chapters.size
            }
        }
        var done = 0
        emit(SearchIndexEvent.Progress(done, totalChapters))

        for (gref in root.guides) {
            val manifest = withContext(Dispatchers.IO) { repo.loadGuideManifestByPath(gref.manifestPath) }
            val guideDir  = repo.guideDirFromManifestPath(gref.manifestPath)
            val guideName = manifest.guide.title

            Log.d(TAG_INDEXER, "▶︎ Guía '$guideName' (${manifest.chapters.size} capítulos)")

            for (entry in manifest.chapters) {
                val chPath = chapterPathOf(entry)
                if (chPath.isNullOrBlank()) {
                    done++
                    emit(SearchIndexEvent.Progress(done, totalChapters))
                    continue
                }
                val cacheKey = "$guideDir/$chPath"

                val chIndex = withContext(Dispatchers.IO) {
                    chapterCache[cacheKey] ?: run {
                        val content = repo.loadChapterContentOrNull(guideDir, chPath)
                        if (content == null) null else {
                            val blocks = withContext(Dispatchers.Default) { extractBlocks(content, query.flags) }
                            val idx = ChapterIndex(
                                chapterSlug = content.chapter.slug,
                                chapterPath = chPath,
                                blocks = blocks
                            )
                            chapterCache[cacheKey] = idx
                            idx
                        }
                    }
                }

                if (chIndex != null) {
                    val hitsForChapter = withContext(Dispatchers.Default) {
                        buildHitsForChapter(guideName, chIndex, query)
                    }
                    if (hitsForChapter.isNotEmpty()) {
                        Log.d(TAG_INDEXER, "   · ${chIndex.chapterSlug} -> ${hitsForChapter.sumOf { it.matchesCount }} coincidencias")
                        emit(SearchIndexEvent.PartialResults(hitsForChapter))
                    }
                }

                done++
                emit(SearchIndexEvent.Progress(done, totalChapters))
            }
        }

        emit(SearchIndexEvent.Done)
    }

    /** Limpia la cache LRU (liberar memoria) */
    fun clearIndexCache() {
        Log.d(TAG_INDEXER, "Limpiando cache de capítulos (size=${chapterCache.size})")
        chapterCache.clear()
    }

    // ============================================================
    //  Núcleo compartido (por guía)
    // ============================================================
    private suspend fun FlowCollector<SearchIndexEvent>.emitAllInternal(
        guideDir: String,
        guideSlug: String,
        chapters: List<ChapterEntry>,
        query: SearchQuery
    ) {
        val total = chapters.size
        var done = 0
        emit(SearchIndexEvent.Progress(done, total))

        for (entry in chapters) {
            val chPath = chapterPathOf(entry)
            if (chPath.isNullOrBlank()) {
                done++
                emit(SearchIndexEvent.Progress(done, total))
                continue
            }

            val cacheKey = "$guideDir/$chPath"
            val chIndex = withContext(Dispatchers.IO) {
                chapterCache[cacheKey] ?: run {
                    val content = repo.loadChapterContent(guideDir, chPath)
                    val blocks  = withContext(Dispatchers.Default) { extractBlocks(content, query.flags) }
                    val idx = ChapterIndex(
                        chapterSlug = content.chapter.slug,
                        chapterPath = chPath,
                        blocks = blocks
                    )
                    chapterCache[cacheKey] = idx
                    Log.d(TAG_INDEXER, "   · Indexado '${idx.chapterSlug}' / blocks=${idx.blocks.size} / path=$chPath")
                    idx
                }
            }

            val hitsForChapter = withContext(Dispatchers.Default) {
                buildHitsForChapter(guideSlug, chIndex, query)
            }
            if (hitsForChapter.isNotEmpty()) {
                emit(SearchIndexEvent.PartialResults(hitsForChapter))
            }

            done++
            emit(SearchIndexEvent.Progress(done, total))
        }

        emit(SearchIndexEvent.Done)
    }

    // ============================================================
    //  Extractores y matching
    // ============================================================

    /**
     * Extrae bloques de texto “planos” por sección:
     * - TextSection: body y footnote (si existen) como bloques separados (sufijos #body / #footnote).
     * - TableSection: título, cada celda, group/operator, footnote (sufijos estables).
     * - ImageSection: título/caption/alt (bloque único #caption si no están vacíos).
     *
     * Nota: no mezclamos heading/body/footnote en un mismo bloque para que los rangos
     * se pinten EXACTOS en el widget que los renderiza.
     */
    private fun extractBlocks(content: ChapterContent, flags: SearchFlags): List<BlockIndex> {
        val secs = content.content.sections
        val out = ArrayList<BlockIndex>(secs.size * 4)

        secs.forEachIndexed { i, section ->
            when (section) {
                is TextSection -> {
                    val sid = section.id ?: "sec-$i"
                    section.body?.trim()?.takeIf { it.isNotEmpty() }?.let { body ->
                        out += BlockIndex("${sid}#body", SectionType.TEXT, body, body.normalizeForSearch(flags))
                    }
                    section.footnote?.trim()?.takeIf { it.isNotEmpty() }?.let { foot ->
                        out += BlockIndex("${sid}#footnote", SectionType.TEXT, foot, foot.normalizeForSearch(flags))
                    }
                }

                is TableSection -> {
                    val sid = section.id ?: "sec-$i-${section::class.simpleName}"
                    section.title?.trim()?.takeIf { it.isNotEmpty() }?.let { t ->
                        out += BlockIndex("${sid}#title", SectionType.TABLE, t, t.normalizeForSearch(flags))
                    }
                    section.rows.forEachIndexed { rIdx, row ->
                        row.group?.trim()?.takeIf { it.isNotEmpty() }?.let { g ->
                            out += BlockIndex("${sid}#r${rIdx}group", SectionType.TABLE, g, g.normalizeForSearch(flags))
                        }
                        row.operator?.trim()?.takeIf { it.isNotEmpty() }?.let { op ->
                            out += BlockIndex("${sid}#r${rIdx}op", SectionType.TABLE, op, op.normalizeForSearch(flags))
                        }
                        val values = row.cells.values.toList()
                        values.forEachIndexed { cIdx, cell ->
                            val txt = (cell ?: "").trim()
                            if (txt.isNotEmpty()) {
                                out += BlockIndex("${sid}#r${rIdx}c${cIdx}", SectionType.TABLE, txt, txt.normalizeForSearch(flags))
                            }
                        }
                    }
                    section.footnote?.trim()?.takeIf { it.isNotEmpty() }?.let { f ->
                        out += BlockIndex("${sid}#footnote", SectionType.TABLE, f, f.normalizeForSearch(flags))
                    }
                }

                is ImageSection -> {
                    val sid = section.id ?: "sec-$i-${section::class.simpleName}"
                    val raw = buildString {
                        section.title?.let   { append(it).append('\n') }
                        section.caption?.let { append(it).append('\n') }
                        section.alt?.let     { append(it) }
                    }.trim()
                    if (raw.isNotEmpty()) {
                        out += BlockIndex("${sid}#caption", SectionType.IMAGE, raw, raw.normalizeForSearch(flags))
                    }
                }

                else -> Unit
            }
        }

        Log.d(TAG_INDEXER, "extractBlocks(): sections=${secs.size} -> blocks=${out.size}")
        return out
    }
    // === Reemplaza TODO este método por el siguiente ===
    private fun buildHitsForChapter(
        guideSlug: String,
        chapter: ChapterIndex,
        query: SearchQuery
    ): List<SearchHit> {
        val hits = ArrayList<SearchHit>()

        for (b in chapter.blocks) {
            // 1) Rangos en NORMALIZADO (esta función ya es accent-insensitive/case-insensitive)
            var normRanges: List<IntRange> = findAllMatches(b.normalized, query.normalized)

            // 2) Fallback puntual: indexOf simple en normalizado
            if (normRanges.isEmpty()) {
                val ntext = b.normalized.normalized
                val idx = ntext.indexOf(query.normalized)
                if (idx >= 0) {
                    normRanges = listOf(idx..(idx + query.normalized.length - 1))
                }
            }
            if (normRanges.isEmpty()) continue

            // 3) Mapear rangos del NORMALIZADO → ORIGINAL usando el IntArray 'map'
            val idxMap: IntArray = b.normalized.map
            val lastNorm = idxMap.lastIndex

            val origRanges: List<IntRange> = normRanges.map { r ->
                val sNorm = r.first.coerceIn(0, lastNorm)
                val eNorm = r.last.coerceIn(sNorm, lastNorm)
                val sOrig = idxMap.getOrElse(sNorm) { 0 }
                val eOrig = idxMap.getOrElse(eNorm) { sOrig }
                sOrig..eOrig
            }.filter { it.first <= it.last } // defensa por si algo raro

            if (origRanges.isEmpty()) continue

            // 4) Preview usando el PRIMER rango del TEXTO ORIGINAL
            val preview = buildPreview(b.originalText, origRanges.first())

            // 5) Emitir hit
            hits += SearchHit(
                guideSlug   = guideSlug,
                chapterSlug = chapter.chapterSlug,
                chapterPath = chapter.chapterPath,
                sectionId   = b.sectionId,
                sectionType = b.sectionType,
                matchRanges = origRanges, // ← SIEMPRE en índices de ORIGINAL
                preview     = preview
            )
        }
        return hits
    }


    // ============================================================
    //  Helpers
    // ============================================================

    /** Compatibilidad con modelos: path / chapterPath / file / manifestPath */
    private fun chapterPathOf(entry: ChapterEntry): String? {
        try { entry.chapterPath?.let { if (it.isNotBlank()) return it } } catch (_: Throwable) {}
        try { entry.path?.let        { if (it.isNotBlank()) return it } } catch (_: Throwable) {}
        try { entry.file?.let        { if (it.isNotBlank()) return it } } catch (_: Throwable) {}
        return runCatching {
            val f = entry.javaClass.getDeclaredField("manifestPath")
            f.isAccessible = true
            (f.get(entry) as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /**
     * Búsqueda acotada a un solo capítulo (API auxiliar).
     */
    fun searchInSingleChapter(
        guideDir: String,
        guideSlugOrTitle: String,
        chapterPath: String,
        query: SearchQuery
    ): Flow<SearchIndexEvent> = flow {
        emit(SearchIndexEvent.Progress(0, 1))

        val cacheKey = "$guideDir/$chapterPath"
        val chIndex = withContext(Dispatchers.IO) {
            chapterCache[cacheKey] ?: run {
                val content = repo.loadChapterContent(guideDir, chapterPath)
                val blocks  = withContext(Dispatchers.Default) { extractBlocks(content, query.flags) }
                val idx = ChapterIndex(
                    chapterSlug = content.chapter.slug,
                    chapterPath = chapterPath,
                    blocks = blocks
                )
                chapterCache[cacheKey] = idx
                idx
            }
        }

        val hitsForChapter = withContext(Dispatchers.Default) {
            buildHitsForChapter(guideSlugOrTitle, chIndex, query)
        }
        if (hitsForChapter.isNotEmpty()) {
            emit(SearchIndexEvent.PartialResults(hitsForChapter))
        }

        emit(SearchIndexEvent.Progress(1, 1))
        emit(SearchIndexEvent.Done)
    }
}
