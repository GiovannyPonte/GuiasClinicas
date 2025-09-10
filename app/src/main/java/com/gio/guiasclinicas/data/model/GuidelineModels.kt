
// package com.gio.guideline.render

@file:Suppress("unused")

package com.gio.guiasclinicas.data.model

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// ---------- Metamodelo mínimo para render ----------

@Serializable
data class LocaleText(
    val es: String? = null,
    val en: String? = null
) {
    fun best(): String = es ?: en ?: ""
}

@Serializable
data class GuidelineFlow(
    val schema: String,
    val id: String,
    val title: LocaleText? = null,
    val version: String? = null,
    val ui_hints: UIHints? = null,
    val terminology: Terminology? = null,
    val variables: List<Variable>? = null,
    val calculations: List<Calculation>? = null,
    val nodes: List<Node> = emptyList(),
    val outputs: Outputs? = null
)

@Serializable
data class UIHints(
    val theme: String? = null,
    val layout: LayoutHints? = null,
    val shapes: Shapes? = null,
    val show_teaching_default: Boolean? = null
)

@Serializable
data class LayoutHints(
    val direction: String? = "TB",
    val spacing_x: Int = 64,
    val spacing_y: Int = 56,
    val cluster_gap: Int = 32
)

@Serializable
data class Shapes(
    val start: String? = "oval",
    val end: String? = "oval",
    val decision: String? = "diamond",
    val form: String? = "roundedRect",
    val score: String? = "card",
    val output: String? = "pill"
)

@Serializable
data class Terminology(
    val units: List<UnitDef>? = null
)

@Serializable
data class UnitDef(
    val name: String,
    val canonical_ucum: String,
    val equivalents: List<UnitEq> = emptyList()
)

@Serializable
data class UnitEq(
    val ucum: String,
    val label: String? = null,
    val factor_to_canonical: Double
)

@Serializable
data class Variable(
    val id: String,
    val label: LocaleText? = null,
    val type: String,
    val input: JsonObject? = null,
    val enum: List<String>? = null,
    val default: String? = null,
    val unit_binding: String? = null
)

@Serializable
data class Calculation(
    val id: String,
    val type: String? = null,            // "score" | null
    val description: String? = null,
    val unit: String? = null,
    val lang: String? = null,            // "jsonlogic"
    val expr: JsonElement? = null,
    val items: List<ScoreItem>? = null,
    val sum: Boolean? = null
)

@Serializable
data class ScoreItem(
    val id: String,
    val label: LocaleText? = null,
    val points: Double = 0.0,
    val condition: JsonElement? = null
)

// ---------- Nodos ----------

@Serializable
data class Transition(
    val `when`: JsonElement? = null,
    val goto: String? = null,
    val label: String? = null          // opcional para rotular arista
)

@Serializable
data class Field(
    val id: String,
    val label: LocaleText? = null,
    val type: String,                  // "boolean" | "number" | "select"
    val options: List<String>? = null
)

@Serializable
data class Teaching(
    val title: LocaleText? = null,
    val text: LocaleText? = null,
    val refs: List<String>? = null
)

@Serializable
data class Node(
    val id: String,
    val type: String,                  // start | form | decision | output | end
    val title: LocaleText? = null,
    val prompt: LocaleText? = null,
    val exposeInKey: Boolean? = null,
    val fields: List<Field>? = null,
    val bind_to: String? = null,
    val transitions: List<Transition>? = null,
    val next: String? = null,
    val recommendation: LocaleText? = null,
    val severity: String? = null,
    val teaching: List<Teaching>? = null
)

@Serializable
data class Outputs(
    val summary_templates: List<SummaryTemplate>? = null
)

@Serializable
data class SummaryTemplate(
    val id: String,
    val locale: String,
    val template: String
)

// ---------- Parser ----------

object GuidelineParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    fun fromAssets(context: Context, relativePath: String): GuidelineFlow {
        context.assets.open(relativePath).use { stream ->
            val txt = stream.bufferedReader().readText()
            return json.decodeFromString(GuidelineFlow.serializer(), txt)
        }
    }
}
