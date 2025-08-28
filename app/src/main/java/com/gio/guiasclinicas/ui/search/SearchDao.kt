package com.gio.guiasclinicas.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SearchDao {
    @Query(
        """
        SELECT rowid, guideSlug, guideTitle, chapterPath, chapterTitle, sectionId,
               snippet(entries, 5, '[', ']', '…', 10) AS preview
        FROM entries
        WHERE entries MATCH :query
        """
    )
    suspend fun search(query: String): List<SearchResult>

    @Insert
    suspend fun insertAll(entries: List<SearchEntity>)

    @Query("DELETE FROM entries")
    suspend fun clearAll()
}