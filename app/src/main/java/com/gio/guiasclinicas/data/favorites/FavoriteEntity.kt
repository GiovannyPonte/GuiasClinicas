package com.gio.guiasclinicas.data.favorites

import androidx.room.*

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["guideDir", "chapterPath"], unique = true)]
)
data class FavoriteChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guideTitle: String,
    val guideDir: String,         // p.ej. ".../Guías AHA de Hipertensión Arterial 2025"
    val chapterTitle: String,
    val chapterSlug: String?,     // para comparar rápido con el capítulo activo
    val chapterPath: String,      // para abrir con repo/selectChapter
    val addedAt: Long = System.currentTimeMillis()
)
