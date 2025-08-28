package com.gio.guiasclinicas.search

import androidx.room.Entity
import androidx.room.Fts5Entity
import androidx.room.PrimaryKey

/**
 * FTS5 entity used by Room to persist searchable content.
 */
@Fts5Entity
@Entity(tableName = "entries")
data class SearchEntity(
    @PrimaryKey(autoGenerate = true)
    val rowid: Int? = null,
    val guideSlug: String,
    val guideTitle: String,
    val chapterPath: String,
    val chapterTitle: String,
    val sectionId: String?,
    val content: String
)