package com.gio.guiasclinicas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.ui.components.TableSectionView
import com.gio.guiasclinicas.ui.components.workflow.WorkflowSectionView
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.theme.*
import androidx.compose.ui.unit.Dp

/** API pública: renderiza un capítulo como acordeón sin tocar JSON. */
@Composable
fun ChapterAccordionView(
    state: ChapterUiState,
    guideDir: String
) {
    when (state) {
        is ChapterUiState.Ready ->
            AccordionBody(
                sections = state.content.content.sections,
                guideDir = guideDir
            )
        is ChapterUiState.Loading -> Text("Cargando...", modifier = Modifier.padding(16.dp))
        is ChapterUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.padding(16.dp))
        ChapterUiState.Idle -> Text("Seleccione un capítulo", modifier = Modifier.padding(16.dp))
    }
}

/** Implementación real del acordeón (privada). */
@Composable
private fun AccordionBody(
    sections: List<ChapterSection>,
    guideDir: String
) {
    // Usamos TU tema de secciones (forma/padding globales) + el tema de acordeón (colores header/cuerpo).
    ChapterSectionThemeProvider {
        AccordionThemeProvider {
            val secTheme = LocalChapterSectionTheme.current
            val accTheme = LocalAccordionTheme.current
            val listState = rememberLazyListState()
            var openKey by rememberSaveable { mutableStateOf<String?>(null) }

            val cardCorner: Dp = accTheme.cornerRadius
            val cardShape = RoundedCornerShape(cardCorner)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(
                    sections,
                    key = { index, item -> item.id ?: "sec-$index-${item::class.simpleName}" }
                ) { index, section ->
                    val key = section.id ?: "sec-$index-${section::class.simpleName}"
                    val expanded = openKey == key

                    if (index > 0) Spacer(Modifier.height(accTheme.itemSpacing))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = accTheme.cardElevation),
                        colors = CardDefaults.cardColors(
                            containerColor = accTheme.headerBg.takeIf { it != Color.Unspecified }
                                ?: MaterialTheme.colorScheme.surface,
                            contentColor = accTheme.headerFg.takeIf { it != Color.Unspecified }
                                ?: MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Column {
                            // ===== Header (clicable), con padding del tema y chevron que rota =====
                            val headerTitle = section.title
                                ?: (section as? TextSection)?.heading
                                ?: (section as? ImageSection)?.caption
                                ?: "Sección ${index + 1}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = accTheme.headerPadH,
                                        top = accTheme.headerPadTop,     // aire ↑ para no “cortar” en el radio
                                        end = accTheme.headerPadH,
                                        bottom = accTheme.headerPadBottom
                                    )
                                    .clickable { openKey = if (expanded) null else key },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                val rotation by animateFloatAsState(
                                    targetValue = if (expanded) 180f else 0f,
                                    label = "chevron"
                                )
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "Contraer" else "Expandir",
                                    modifier = Modifier.rotate(rotation),
                                    tint = accTheme.headerIconTint.takeIf { it != Color.Unspecified }
                                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // ===== Body: superficie propia, con redondeo SOLO inferior =====
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                ChapterTheme {
                                    val bodyTheme = LocalChapterTheme.current
                                    Surface(
                                        color = accTheme.bodyBg.takeIf { it != Color.Unspecified }
                                            ?: bodyTheme.bodyCardBg,
                                        contentColor = accTheme.bodyFg.takeIf { it != Color.Unspecified }
                                            ?: bodyTheme.bodyCardOnBg,
                                        shape = RoundedCornerShape(
                                            topStart = 0.dp, topEnd = 0.dp,
                                            bottomStart = cardCorner, bottomEnd = cardCorner
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            RenderAccordionSection(
                                                section = section,
                                                guideDir = guideDir,
                                                titleAlreadyShown = !section.title.isNullOrBlank() ||
                                                        (section is TextSection && !section.heading.isNullOrBlank())
                                            )
                                        }
                                    }
                                }
                            }

                            // Auto‑scroll al abrir
                            LaunchedEffect(expanded) {
                                if (expanded) listState.animateScrollToItem(index)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderAccordionSection(
    section: ChapterSection,
    guideDir: String,
    titleAlreadyShown: Boolean
) {
    when (section) {
        is TextSection -> {
            // Evitar duplicar el heading si ya se mostró en el header
            if (!titleAlreadyShown && !section.heading.isNullOrBlank()) {
                Text(section.heading!!, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }

            section.body?.let { md ->
                // Reutiliza el markdown del WorkflowRenderer
                val base = MaterialTheme.typography.bodyMedium
                androidx.compose.material3.ProvideTextStyle(
                    value = base.copy(lineHeight = 20.sp, fontSize = 15.sp)
                ) {
                    com.gio.guiasclinicas.ui.components.markdown.MarkdownText(md)
                }
            }

            section.footnote?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalChapterTheme.current.footnoteText
                )
            }
        }

        is TableSection -> {
            TableSectionView(section)  // misma firma que ya usas
        }

        is ImageSection -> {
            ImageSectionView(
                section = section,
                captionText = section.caption?.let { androidx.compose.ui.text.AnnotatedString(it) },
                footnoteText = section.footnote?.let { androidx.compose.ui.text.AnnotatedString(it) }
            )
        }

        is WorkflowSection -> {
            WorkflowSectionView(
                relativePath = "${guideDir.trimEnd('/')}/${section.path}",
                title = section.title ?: "Evaluación paso a paso",
                startButtonLabel = section.startButtonLabel ?: "Comenzar evaluación",
                locale = section.locale ?: "es"
            )
        }

        is OrganigramaSection -> {
            val rel = guideDir.trimEnd('/') + "/" + section.path.trimStart('/')
            GuidelineDiagramFromAssets(
                relativePath = rel,
                locale = section.locale ?: "es",
                showKeyTablesBelow = section.showKeyTables ?: true,
                embedded = true
            )
        }
    }
}
