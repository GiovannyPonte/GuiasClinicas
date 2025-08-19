package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.model.*
import org.json.JSONArray
import org.json.JSONObject

object GuidesRepository {

    private const val BASE = "clinical_guidelines_db/"
    private const val ROOT = BASE + "root_manifest.json"

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    // --- ROOT ---

    fun loadRootManifest(context: Context): RootManifest {
        val txt = readAsset(context, ROOT)
        val obj = JSONObject(txt)
        val arr = obj.getJSONArray("guides")
        val guides = (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            GuideRef(
                slug = g.getString("slug"),
                title = g.getString("title"),
                folder = g.getString("folder"),
                manifestPath = g.getString("manifestPath")
            )
        }
        return RootManifest(guides)
    }

    fun findGuideBySlug(context: Context, slug: String): GuideRef? {
        val root = loadRootManifest(context)
        return root.guides.firstOrNull { it.slug == slug }
    }

    // --- GUIDE MANIFEST ---

    fun loadGuideManifestByPath(context: Context, manifestPath: String): GuideManifest {
        // manifestPath viene relativo a BASE (ej: "Guías .../manifest.json")
        val txt = readAsset(context, BASE + manifestPath)
        val obj = JSONObject(txt)
        val arr = obj.getJSONArray("chapters")
        val chapters = (0 until arr.length()).map { i ->
            val c = arr.getJSONObject(i)
            GuideChapter(
                id = c.getString("id"),
                title = c.getString("title"),
                contentPath = resolveContentPath(c)
            )
        }
        return GuideManifest(
            title = obj.getString("title"),
            version = obj.getString("version"),
            chapters = chapters
        )
    }

    private fun resolveContentPath(chapterObj: JSONObject): String {
        // Soporta varias claves posibles
        val keys = listOf("contentPath", "jsonPath", "path", "file", "content")
        for (k in keys) {
            if (chapterObj.has(k)) return chapterObj.getString(k)
        }
        // Fallback: usar id.json
        val id = chapterObj.optString("id", "chapter")
        return "$id.json"
    }

    fun guideDirFromManifestPath(manifestPath: String): String {
        // ej: "Guías AHA .../manifest.json" -> "Guías AHA ..."
        val idx = manifestPath.lastIndexOf('/')
        return if (idx >= 0) manifestPath.substring(0, idx) else ""
    }

    // --- CHAPTER CONTENT ---

    fun loadChapterContent(context: Context, guideDir: String, contentPath: String): ChapterContent {
        // Si contentPath ya incluye una subcarpeta, úsalo tal cual bajo BASE
        val full = if (contentPath.contains("/")) {
            BASE + contentPath
        } else {
            // Si es solo "intro.json", asumir que está dentro de la carpeta de la guía:
            "$BASE$guideDir/$contentPath"
        }
        val raw = readAsset(context, full)
        return ChapterContent(rawJson = raw)
    }
}
