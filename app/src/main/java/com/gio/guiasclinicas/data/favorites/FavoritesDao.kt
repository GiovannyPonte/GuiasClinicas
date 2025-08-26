package com.gio.guiasclinicas.data.favorites

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: FavoriteChapterEntity): Long

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM favorites WHERE guideDir = :guideDir AND chapterPath = :chapterPath")
    suspend fun exists(guideDir: String, chapterPath: String): Int

    @Query("DELETE FROM favorites WHERE guideDir = :guideDir AND chapterPath = :chapterPath")
    suspend fun deleteByKey(guideDir: String, chapterPath: String)
}
