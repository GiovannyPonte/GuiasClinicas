package com.gio.guiasclinicas.search

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Repository responsible for building and querying the FTS index.
 */
class SearchRepository(private val context: Context) {
    private val dao = SearchDatabase.get(context).searchDao()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Rebuilds the search index using the JSON files shipped in assets.
     */
    suspend fun buildIndex() = withContext(Dispatchers.IO) {
        dao.clearAll()
        val entries = mutableListOf<SearchEntity>()
        val root = readJson("clinical_guidelines_db/root_manifest.json").jsonObject
        val guides = root["guides"] as? JsonArray ?: JsonArray(emptyList())
        for (guideEl in guides) {
            val guideObj = guideEl.jsonObject
            val guideSlug = guideObj["slug"]?.jsonPrimitive?.content ?: continue
            val guideTitle = guideObj["title"]?.jsonPrimitive?.content ?: guideSlug
            val folder = guideObj["folder"]?.jsonPrimitive?.content ?: continue
            val guideManifestPath = guideObj["manifestPath"]?.jsonPrimitive?.content ?: continue
            val guideManifest = readJson(guideManifestPath).jsonObject
            val chapters = guideManifest["chapters"] as? JsonArray ?: JsonArray(emptyList())
            for (chapterEl in chapters) {
                val chapterObj = chapterEl.jsonObject
                val chapterTitle = chapterObj["title"]?.jsonPrimitive?.content ?: continue
                val chapterManifestPath = chapterObj["manifestPath"]?.jsonPrimitive?.content ?: continue
                val chapterPath = "clinical_guidelines_db/$folder/$chapterManifestPath"
                val chapterJson = readJson(chapterPath)
                val text = extractText(chapterJson)
                entries.add(
                    SearchEntity(
                        guideSlug = guideSlug,
                        guideTitle = guideTitle,
                        chapterPath = chapterPath,
                        chapterTitle = chapterTitle,
                        sectionId = null,
                        content = text
                    )
                )
            }
        }
        dao.insertAll(entries)
    }

    suspend fun search(query: String): List<SearchResult> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return emptyList()
        return dao.search(cleaned)
    }

    private fun readJson(path: String): JsonElement {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.parseToJsonElement(text)
    }

    private fun extractText(element: JsonElement): String = when (element) {
        is JsonObject -> element.values.joinToString(" ") { extractText(it) }
        is JsonArray -> element.joinToString(" ") { extractText(it) }
        else -> element.jsonPrimitive.takeIf { it.isString }?.content ?: ""
    }
}