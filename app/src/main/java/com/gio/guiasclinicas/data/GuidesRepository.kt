package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.model.ChapterContent
import com.gio.guiasclinicas.data.model.GuideManifest
import com.gio.guiasclinicas.data.model.GuideRef
import com.gio.guiasclinicas.data.model.RootManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class GuidesRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type" // <-- coincide con @JsonClassDiscriminator("type")
    }


/** Resuelve una ruta en assets probando raíz y clinical_guidelines_db/ */
private fun resolveAssetPath(path: String): String {
val candidates = listOf(
path,
"clinical_guidelines_db/$path"
)
return candidates.firstOrNull { existsInAssets(it) }
?: error("No se encontró en assets: $path (probé ${candidates.joinToString()})")
}

/** Carga el root manifest con el listado de guías disponibles. */
suspend fun loadRootManifest(): RootManifest = withContext(Dispatchers.IO) {
val path = resolveAssetPath("root_manifest.json")
context.assets.open(path).bufferedReader().use { reader ->
json.decodeFromString<RootManifest>(reader.readText())
}
}

/** Busca una guía por slug. */
suspend fun findGuideBySlug(slug: String): GuideRef? {
val root = loadRootManifest()
return root.guides.find { it.slug == slug }
}

/** Carga el manifest.json de una guía (acepta rutas relativas o absolutas en assets). */
suspend fun loadGuideManifestByPath(path: String): GuideManifest = withContext(Dispatchers.IO) {
val real = resolveAssetPath(path)
context.assets.open(real).bufferedReader().use { reader ->
json.decodeFromString<GuideManifest>(reader.readText())
}
}

/** Devuelve el directorio base de la guía a partir de su manifestPath. */
fun guideDirFromManifestPath(path: String): String =
resolveAssetPath(path).substringBeforeLast('/')

/** Carga el contenido de un capítulo. */
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
