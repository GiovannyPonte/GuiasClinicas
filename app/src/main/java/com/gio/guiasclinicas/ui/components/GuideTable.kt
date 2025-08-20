package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.ui.components.table.AutoResizeText
import com.gio.guiasclinicas.ui.theme.LocalTableTheme

@Composable
fun TableSectionView(section: TableSection) {
    val cols = section.columns.orEmpty()
    val rows = section.rows.orEmpty()
    if (cols.isEmpty()) return

    val density = LocalDensity.current
    val hScroll = rememberScrollState()
    val gutter = 0.dp

    fun isSmallColumn(label: String, key: String): Boolean {
        val k = key.lowercase(); val l = label.lowercase()
        return k == "cor" || k == "loe" || k == "op" || l == "cor" || l == "loe" || l == "op"
    }

    val smallMinW = 56.dp
    val flexMinW  = 96.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Título
        section.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val containerW = this@BoxWithConstraints.maxWidth
                val smallCount = cols.count { isSmallColumn(it.label, it.key) }
                val flexCount  = cols.size - smallCount

                val smallTotal = smallMinW * smallCount.toFloat()
                val freeForFlex = (containerW - smallTotal).coerceAtLeast(0.dp)

                // Cada columna flex toma al menos flexMinW; si hay más, se reparte homogéneo
                val flexW = if (flexCount > 0) {
                    val base = (freeForFlex / flexCount.toFloat())
                    maxOf(flexMinW, base)
                } else 0.dp

                val tableW = (smallTotal + flexW * flexCount.toFloat()).coerceAtLeast(containerW)
                val colWidths: List<Dp> = cols.map { c ->
                    if (isSmallColumn(c.label, c.key)) smallMinW else flexW
                }

                Box(
                    modifier = Modifier
                        .width(tableW)
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ===== Encabezado =====
                        Row(modifier = Modifier.width(tableW)) {
                            cols.forEachIndexed { index, col ->
                                val cw = colWidths[index]
                                val small = isSmallColumn(col.label, col.key)
                                HeaderCell(
                                    text = col.label,
                                    width = cw,
                                    center = small,
                                    hPad = if (small) 8.dp else LocalTableTheme.current.cellPaddingH.dp,
                                    minH = LocalTableTheme.current.cellMinHeightDp.dp
                                )
                            }
                        }

                        // ===== Filas =====
                        var lastGroup: String? = null
                        rows.forEach { r ->
                            if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                                lastGroup = r.group
                                Row(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                    Text(
                                        text = r.group.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LocalTableTheme.current.groupLabelColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.width(tableW),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                cols.forEachIndexed { idx, col ->
                                    val cw = colWidths[idx]
                                    val small = isSmallColumn(col.label, col.key)
                                    val text = r.cells[col.key].orEmpty()

                                    if (small) {
                                        BodyCellText(
                                            text = text,
                                            width = cw,
                                            center = true,
                                            hPad = 8.dp,
                                            minH = LocalTableTheme.current.cellMinHeightDp.dp
                                        )
                                    } else {
                                        val cwPx = with(density) { cw.toPx() }.toInt()
                                        BodyCellAuto(
                                            text = text,
                                            width = cw,
                                            maxWidthPx = cwPx,
                                            hPad = LocalTableTheme.current.cellPaddingH.dp,
                                            minH = LocalTableTheme.current.cellMinHeightDp.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nota al pie
        section.footnote?.let { note ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Celdas ----------

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    center: Boolean,
    hPad: Dp,
    minH: Dp
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = minH)
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(theme.headerBg, shape)
            .padding(horizontal = hPad, vertical = theme.cellPaddingV.dp),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            softWrap = true
        )
    }
}

@Composable
private fun BodyCellText(
    text: String,
    width: Dp,
    center: Boolean,
    hPad: Dp,
    minH: Dp
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = minH)
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(theme.cellBg, shape)
            .padding(horizontal = hPad, vertical = theme.cellPaddingV.dp),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            softWrap = true
        )
    }
}

@Composable
private fun BodyCellAuto(
    text: String,
    width: Dp,
    maxWidthPx: Int,
    hPad: Dp,
    minH: Dp
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = minH)
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(theme.cellBg, shape)
            .padding(horizontal = hPad, vertical = theme.cellPaddingV.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AutoResizeText(
            text = text,
            maxWidthPx = maxWidthPx,
            style = MaterialTheme.typography.bodyMedium,
            maxFontSize = theme.textMaxSp.sp,
            minFontSize = theme.textMinSp.sp,
            maxLines = Int.MAX_VALUE
        )
    }
}
