package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ChapterContentView(rawJson: String) {
    val obj = runCatching { JSONObject(rawJson) }.getOrNull()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll)
    ) {
        if (obj == null) {
            Text("Contenido no disponible", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        val title = obj.optString("title", "")
        if (title.isNotBlank()) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }

        val summary = obj.optString("summary", "")
        if (summary.isNotBlank()) {
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
        }

        // Secciones con puntos
        val sections = obj.optJSONArray("sections")
        if (sections != null && sections.length() > 0) {
            for (i in 0 until sections.length()) {
                val s = sections.getJSONObject(i)
                val stitle = s.optString("title", "")
                if (stitle.isNotBlank()) {
                    Text(stitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                }
                val points: JSONArray? = s.optJSONArray("points") ?: s.optJSONArray("items")
                if (points != null) {
                    for (j in 0 until points.length()) {
                        val line = points.optString(j)
                        Text("• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
            // Fallback: imprime JSON bonito si no hay estructura conocida
            Text(obj.toString(2), style = MaterialTheme.typography.bodySmall)
        }
    }
}
