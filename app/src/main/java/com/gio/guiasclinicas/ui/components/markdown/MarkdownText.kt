package com.gio.guiasclinicas.ui.components.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.ui.components.workflow.parseBold
import com.gio.guiasclinicas.ui.theme.LocalChapterTheme

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalChapterTheme.current.bodyCardOnBg,
    onHintClick: ((String) -> Unit)? = null // se mantiene para el workflow; en capítulos normalmente null
) {
    // --- Negritas básicas: **texto**
    fun buildBoldAnnotated(s: String): AnnotatedString {
        val bold = Regex("\\*\\*(.+?)\\*\\*")
        return buildAnnotatedString {
            var idx = 0
            for (m in bold.findAll(s)) {
                append(s.substring(idx, m.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(m.groupValues[1])
                }
                idx = m.range.last + 1
            }
            if (idx < s.length) append(s.substring(idx))
        }
    }

    // Construye una línea con **negritas** + "píldoras" {i:id} → ⓘ clicable
    @Composable
    fun buildAnnotatedWithHints(s: String): AnnotatedString {
        // Soporta ambos formatos: {i:id}  y  [[?id]]
        val hintRegex = Regex("(\\{i:([A-Za-z0-9_\\-]+)\\}|\\[\\[\\?([A-Za-z0-9_\\-]+)\\]\\])")
        val b = AnnotatedString.Builder()
        var last = 0
        val hintColor = MaterialTheme.colorScheme.primary

        for (m in hintRegex.findAll(s)) {
            val before = s.substring(last, m.range.first)
            b.append(parseBold(before))

            // id puede venir en el grupo 2 (formato {i:id}) o en el 3 (formato [[?id]])
            val id = m.groups[2]?.value?.takeIf { it.isNotEmpty() }
                ?: m.groups[3]?.value
                ?: continue

            b.pushStringAnnotation(tag = "HINT", annotation = id)
            b.pushStyle(
                SpanStyle(
                    color = hintColor,
                    fontWeight = FontWeight.SemiBold,
                    // opcional: un fondo suave para simular una “píldora”
                    background = hintColor.copy(alpha = 0.12f)
                )
            )
            b.append(" ? ")
            b.pop() // style
            b.pop() // annotation

            last = m.range.last + 1
        }
        b.append(parseBold(s.substring(last)))
        return b.toAnnotatedString()
    }


    @Composable
    fun InlineText(line: String) {
        val annotated = buildAnnotatedWithHints(line)
        if (onHintClick == null) {
            Text(
                text = annotated,
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // Hace clicables sólo los tramos anotados como HINT
            ClickableText(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium.copy(color = color),
                onClick = { offset ->
                    annotated.getStringAnnotations("HINT", offset, offset)
                        .firstOrNull()
                        ?.let { ann -> onHintClick(ann.item) }
                }
            )
        }
    }

    // --- Bloques soportados ---
    val bulletRegex  = Regex("""^\s*([\-*•])\s+(.*)$""")
    val orderedRegex = Regex("""^\s*(\d+)[\.\)]\s+(.*)$""")
    val headingRegex = Regex("""^\s*(#{1,6})\s+(.*)$""")
    val hrRegex      = Regex("""^\s*([*\-_])\1\1+\s*$""")

    val lines = remember(text) { text.replace("\r\n", "\n").split('\n') }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val t = raw.trim()

            when {
                t.isBlank() -> { i++; Spacer(Modifier.height(0.dp)) }
                hrRegex.matches(t) -> { Divider(); i++ }
                headingRegex.matchEntire(t) != null -> {
                    val m = headingRegex.matchEntire(t)!!
                    val level = m.groupValues[1].length
                    val content = m.groupValues[2]
                    val style = when (level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        3 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    Text(content, color = color, style = style)
                    i++
                }
                bulletRegex.matchEntire(t) != null -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size) {
                        val m = bulletRegex.matchEntire(lines[i].trim()) ?: break
                        items += m.groupValues[2]
                        i++
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = color, modifier = Modifier.padding(end = 6.dp))
                                InlineText(item)
                            }
                        }
                    }
                }
                orderedRegex.matchEntire(t) != null -> {
                    val first = orderedRegex.matchEntire(t)!!
                    val start = first.groupValues[1].toIntOrNull() ?: 1
                    val items = mutableListOf<String>()
                    items += first.groupValues[2]
                    i++
                    while (i < lines.size) {
                        val m = orderedRegex.matchEntire(lines[i].trim()) ?: break
                        items += m.groupValues[2]; i++
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEachIndexed { idx, item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("${start + idx}.", color = color, modifier = Modifier.padding(end = 6.dp))
                                InlineText(item)
                            }
                        }
                    }
                }
                else -> {
                    val para = StringBuilder(t)
                    i++
                    while (i < lines.size) {
                        val look = lines[i].trim()
                        if (
                            look.isBlank() ||
                            headingRegex.matches(look) ||
                            bulletRegex.matches(look) ||
                            orderedRegex.matches(look) ||
                            hrRegex.matches(look)
                        ) break
                        para.append(' ').append(look); i++
                    }
                    InlineText(para.toString())
                }
            }
        }
    }
}
