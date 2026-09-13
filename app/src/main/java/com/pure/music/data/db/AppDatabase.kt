package com.pure.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库，持久化收藏、歌单和最近播放记录。
 * 数据库升级通过显式 Migration 保留已有用户数据。
 */
@Database(
    entities = [FavoriteEntity::class, PlaylistEntity::class, PlayHistoryEntity::class, SongEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(LongListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): PlayHistoryDao
    abstract fun songsDao(): SongsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "puremusic.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS play_history (song_id INTEGER NOT NULL PRIMARY KEY, played_at INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS songs (
                        song_id INTEGER NOT NULL PRIMARY KEY,
                        uri TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        album_id INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        size INTEGER NOT NULL,
                        date_added INTEGER NOT NULL,
                        date_modified INTEGER NOT NULL,
                        track_number INTEGER NOT NULL,
                        path TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
