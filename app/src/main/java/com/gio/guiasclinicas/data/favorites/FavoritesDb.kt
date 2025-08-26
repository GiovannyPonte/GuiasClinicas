package com.gio.guiasclinicas.data.favorites

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteChapterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FavoritesDb : RoomDatabase() {
    abstract fun favorites(): FavoritesDao

    companion object {
        @Volatile private var INSTANCE: FavoritesDb? = null
        fun get(ctx: Context): FavoritesDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    FavoritesDb::class.java,
                    "favorites.db"
                ).build().also { INSTANCE = it }
            }
    }
}
