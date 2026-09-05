package com.pure.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库，管理收藏和歌单的持久化。
 * 使用双检锁单例模式，线程安全。
 */
@Database(
    entities = [FavoriteEntity::class, PlaylistEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(LongListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "puremusic.db"
                ).build().also { instance = it }
            }
        }
    }
}
