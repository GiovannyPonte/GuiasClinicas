package com.gio.guiasclinicas.data.repo

import android.content.Context
import com.gio.guiasclinicas.data.repo.GuideRef
import com.gio.guiasclinicas.data.repo.RootManifest
import org.json.JSONArray
import org.json.JSONObject

object GuidesRepository {
    private const val ROOT_PATH = "clinical_guidelines_db/root_manifest.json"

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun loadRootManifest(context: Context): RootManifest {
        val text = readAsset(context, ROOT_PATH)
        val obj = JSONObject(text)
        val guidesArr: JSONArray = obj.getJSONArray("guides")

        val guides = (0 until guidesArr.length()).map { i ->
            val g = guidesArr.getJSONObject(i)
            GuideRef(
                slug = g.getString("slug"),
                title = g.getString("title")
            )
        }
        return RootManifest(guides = guides)
    }
}

