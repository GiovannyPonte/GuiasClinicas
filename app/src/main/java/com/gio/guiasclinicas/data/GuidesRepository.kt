package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.model.*  // <-- trae ChapterContent, ChapterEntry, GuideManifest, etc.
import org.json.JSONObject

object GuidesRepository {

    private const val BASE = "clinical_guidelines_db/"
    private const val ROOT = BASE + "root_manifest.json"

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

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

    // Lee tu manifest.json de la guía con la estructura que enviaste
    fun loadGuideManifestByPath(context: Context, manifestPath: String): GuideManifest {
        val txt = readAsset(context, BASE + manifestPath)
        val obj = JSONObject(txt)

        val guideMeta = obj.getJSONObject("guide").let { g ->
            GuideMeta(
                slug = g.getString("slug"),
                title = g.getString("title"),
                version = g.getString("version"),
                publishedAt = g.getString("publishedAt"),
                organizations = g.getJSONArray("organizations").toListString(),
                status = g.getString("status"),
                locale = g.getString("locale"),
                changelog = g.optString("changelog", ""),
                features = g.optJSONArray("features")?.toListString() ?: emptyList()
            )
        }

        val chapters = obj.getJSONArray("chapters").let { chArr ->
            (0 until chArr.length()).map { i ->
                val c = chArr.getJSONObject(i)
                ChapterEntry(
                    slug = c.getString("slug"),
                    title = c.getString("title"),
                    order = c.getInt("order"),
                    folder = c.getString("folder"),
                    manifestPath = c.getString("manifestPath"),
                    hash = c.optString("hash", "")
                )
            }.sortedBy { it.order }
        }

        val assets = obj.optJSONObject("assets")?.let { a ->
            GuideAssets(
                images = a.optJSONArray("images")?.toListString() ?: emptyList(),
                documents = a.optJSONArray("documents")?.toListString() ?: emptyList()
            )
        } ?: GuideAssets()

        return GuideManifest(
            schemaVersion = obj.getString("schemaVersion"),
            guide = guideMeta,
            chapters = chapters,
            assets = assets
        )
    }

    fun guideDirFromManifestPath(manifestPath: String): String {
        val idx = manifestPath.lastIndexOf('/')
        return if (idx >= 0) manifestPath.substring(0, idx) else ""
    }

    // Devuelve TU ChapterContent (importado desde model)
    fun loadChapterContent(context: Context, guideDir: String, contentPath: String): ChapterContent {
        val full = if ('/' in contentPath) {
            BASE + contentPath
        } else {
            "$BASE$guideDir/$contentPath"
        }
        val raw = readAsset(context, full)
        return ChapterContent(rawJson = raw)
    }

    // helpers
    private fun org.json.JSONArray.toListString(): List<String> =
        (0 until length()).map { getString(it) }
}
