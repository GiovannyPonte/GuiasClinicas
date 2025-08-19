package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.model.ChapterContent
import com.gio.guiasclinicas.data.model.GuideManifest
import com.gio.guiasclinicas.data.model.GuideRef
import com.gio.guiasclinicas.data.model.RootManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Repositorio para leer JSON desde /assets.
 *
 * Estructura típica en /assets:
 * - root_manifest.json   (o dentro de clinical_guidelines_db/)
 * - clinical_guidelines_db/
 *     └─ <Nombre de Guía>/
 *         ├─ manifest.json
 *         └─ capitulos/
 *            ├─ capitulo_concepto.json
 *            └─ capitulo_diagnostico.json
 */
class GuidesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Carga el root manifest con el listado de guías disponibles. */
    suspend fun loadRootManifest(): RootManifest = withContext(Dispatchers.IO) {
        val candidates = listOf(
            "root_manifest.json",
            "clinical_guidelines_db/root_manifest.json"
        )
        val path = candidates.firstOrNull { existsInAssets(it) }
            ?: error("No se encontró root_manifest.json en assets (probé: ${candidates.joinToString()})")

        context.assets.open(path).bufferedReader().use { reader ->
            json.decodeFromString<RootManifest>(reader.readText())
        }
    }

    /** Busca la guía por slug en el root manifest. Devuelve null si no existe. */
    suspend fun findGuideBySlug(slug: String): GuideRef? {
        val root = loadRootManifest()
        return root.guides.find { it.slug == slug }
    }

    /**
     * Carga el manifest.json de una guía dada su ruta dentro de assets.
     * Ej: "clinical_guidelines_db/Guías AHA .../manifest.json"
     */
    suspend fun loadGuideManifestByPath(path: String): GuideManifest = withContext(Dispatchers.IO) {
        context.assets.open(path).bufferedReader().use { reader ->
            json.decodeFromString<GuideManifest>(reader.readText())
        }
    }

    /**
     * Dada la ruta al manifest de guía, devuelve el directorio de la guía.
     * Ej: ".../manifest.json" -> "..."
     */
    fun guideDirFromManifestPath(path: String): String =
        path.substringBeforeLast('/')

    /**
     * Carga el contenido de un capítulo.
     * @param guideDir    Directorio base de la guía (ej: "clinical_guidelines_db/Guías AHA ...")
     * @param chapterPath Ruta relativa del capítulo (ej: "capitulos/capitulo_concepto.json")
     */
    suspend fun loadChapterContent(guideDir: String, chapterPath: String): ChapterContent =
        withContext(Dispatchers.IO) {
            val fullPath = "$guideDir/$chapterPath"
            context.assets.open(fullPath).bufferedReader().use { reader ->
                json.decodeFromString<ChapterContent>(reader.readText())
            }
        }

    // ---------- helpers ----------
    private fun existsInAssets(path: String): Boolean {
        val dir = path.substringBeforeLast('/', "")
        val file = path.substringAfterLast('/')
        val list = context.assets.list(dir) ?: return false
        return list.contains(file)
    }
}
