package com.gio.guiasclinicas.data.favorites

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FavoriteChapter(
    val id: Long,
    val guideTitle: String,
    val guideDir: String,
    val chapterTitle: String,
    val chapterSlug: String?,
    val chapterPath: String
)

class FavoritesRepository(ctx: Context) {
    private val dao = FavoritesDb.get(ctx).favorites()

    val favoritesFlow: Flow<List<FavoriteChapter>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun toggle(guideTitle: String, guideDir: String, chapterTitle: String, chapterSlug: String?, chapterPath: String) {
        if (dao.exists(guideDir, chapterPath) > 0) {
            dao.deleteByKey(guideDir, chapterPath)
        } else {
            dao.upsert(
                FavoriteChapterEntity(
                    guideTitle = guideTitle,
                    guideDir = guideDir,
                    chapterTitle = chapterTitle,
                    chapterSlug = chapterSlug,
                    chapterPath = chapterPath
                )
            )
        }
    }

    suspend fun removeById(id: Long) = dao.deleteById(id)
    suspend fun clearAll() = dao.deleteAll()

    private fun FavoriteChapterEntity.toDomain() = FavoriteChapter(
        id = id,
        guideTitle = guideTitle,
        guideDir = guideDir,
        chapterTitle = chapterTitle,
        chapterSlug = chapterSlug,
        chapterPath = chapterPath
    )
}
