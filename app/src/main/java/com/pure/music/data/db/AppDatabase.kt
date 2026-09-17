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
    version = 5,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN bitrateKbps INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN sampleRateHz INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN channels INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN format TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN lyrics TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN composer TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN comment TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN year INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN disc_number INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN subtitle TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN album_artist TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN lyricist TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN conductor TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN remixer TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN mood TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN bpm TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN isrc TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN copyright TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN label TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN mb_track_id TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN mb_album_id TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN mb_artist_id TEXT")
            }
        }
    }
}
