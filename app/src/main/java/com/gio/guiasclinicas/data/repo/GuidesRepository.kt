package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.model.ChapterContent
import com.gio.guiasclinicas.data.model.GuideManifest
import com.gio.guiasclinicas.data.model.GuideRef
import com.gio.guiasclinicas.data.model.RootManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import android.app.Application
import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/** Elimina comas finales antes de } o ] (p. ej., "..., }" -> "}" ). NO toca texto dentro de strings. */
private fun stripTrailingCommas(json: String): String {
    // Aproximación simple: borra coma seguida de espacios y cierre } o ]
    // (suficiente para tus assets generados).
    val regex = Regex(",\\s*([}\\]])")
    return json.replace(regex, "$1")
}


class GuidesRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type" // <-- coincide con @JsonClassDiscriminator("type")
    }
    // JSON tolerante para fallback
    private val jsonLenient = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        classDiscriminator = "type"
    }
    /** Igual que loadChapterContent pero nunca lanza; devuelve null si no puede parsear. */
    suspend fun loadChapterContentOrNull(guideDir: String, chapterPath: String): ChapterContent? =
        withContext(Dispatchers.IO) {
            val fullPath = "$guideDir/$chapterPath"
            val raw = try {
                context.assets.open(fullPath).bufferedReader().use { it.readText() }
            } catch (io: Exception) {
                Log.e("GuidesRepository", "No se pudo abrir asset: $fullPath", io)
                return@withContext null
            }

            // 1) Intento estricto (rápido)
            runCatching { json.decodeFromString<ChapterContent>(raw) }
                .onSuccess { return@withContext it }
                .onFailure { e -> Log.w("GuidesRepository", "Strict decode falló en $fullPath: ${e.message}") }

            // 2) Fallback tolerante con normalización de secciones "text"
            // 2) Fallback tolerante con normalización de secciones "text"
            val patched = runCatching {
                val cleaned = stripTrailingCommas(raw)               // ⬅️ nuevo
                val el = jsonLenient.parseToJsonElement(cleaned)
                val fixed = patchTextSections(el)
                jsonLenient.decodeFromJsonElement<ChapterContent>(fixed)
            }.getOrElse { e ->
                Log.e("GuidesRepository", "[SKIP] Capítulo inválido (ni siquiera en modo tolerante): $fullPath", e)
                return@withContext null
            }


            patched
        }

    /** Si una clave de texto (heading/body/footnote) viene como array, la junta con saltos de línea. */
    private fun coerceTextFields(obj: JsonObject): JsonObject {
        val m = obj.toMutableMap()
        fun fix(key: String) {
            val v = m[key]
            if (v is JsonArray) {
                val joined = v.mapNotNull { it.jsonPrimitive.contentOrNull }.joinToString("\n")
                m[key] = JsonPrimitive(joined)
            }
        }
        fix("heading"); fix("body"); fix("footnote")
        return JsonObject(m)
    }

    /** Recorre el árbol y si encuentra { "type": "text" } normaliza sus campos texto. */
    private fun patchTextSections(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            val type = element["type"]?.jsonPrimitive?.contentOrNull
            val mm = element.toMutableMap()
            // Normaliza recursivamente la lista de sections si existe
            mm["sections"]?.let { sec ->
                if (sec is JsonArray) {
                    mm["sections"] = JsonArray(sec.map { patchTextSections(it) })
                }
            }
            val fixed = if (type == "text") coerceTextFields(JsonObject(mm)) else JsonObject(mm)
            fixed
        }
        is JsonArray -> JsonArray(element.map { patchTextSections(it) })
        else -> element
    }


/** Resuelve una ruta en assets probando raíz y clinical_guidelines_db/ */
private fun resolveAssetPath(path: String): String {
    // Preferimos el archivo dentro de clinical_guidelines_db
    val candidates = listOf(
        "clinical_guidelines_db/$path",
        path
    )
    val chosen = candidates.firstOrNull { existsInAssets(it) }
        ?: error("No se encontró en assets: $path (probé ${candidates.joinToString()})")
    android.util.Log.d("GuidesRepository", "resolveAssetPath('$path') -> '$chosen'")
    return chosen
}


/** Carga el root manifest con el listado de guías disponibles. */
suspend fun loadRootManifest(): RootManifest = withContext(Dispatchers.IO) {
    val path = resolveAssetPath("root_manifest.json")
    context.assets.open(path).bufferedReader().use { reader ->
        val txt = reader.readText()
        try {
            json.decodeFromString<RootManifest>(txt)
        } catch (e: Exception) {
            if (txt.trimStart().startsWith("\"")) {
                throw IllegalStateException(
                    "El asset '$path' contiene una *cadena* en lugar de un objeto JSON. " +
                            "Asegúrate de que tenga { \"guides\": [ ... ] }."
                )
            }
            throw e
        }
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

// En com/gio/guiasclinicas/data/repo/GuidesRepository.kt

    /** Carga el contenido de un capítulo. Acepta rutas RELATIVAS y ABSOLUTAS en assets. */
    suspend fun loadChapterContent(guideDir: String, chapterPath: String): ChapterContent =
        withContext(Dispatchers.IO) {
            // Si nos pasaron una ruta absoluta (empieza con clinical_guidelines_db/), úsala tal cual.
            val fullPath = if (chapterPath.startsWith("clinical_guidelines_db/")) {
                chapterPath
            } else {
                "$guideDir/$chapterPath"
            }
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