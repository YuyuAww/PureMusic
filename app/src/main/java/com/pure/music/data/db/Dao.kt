package com.pure.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 收藏数据访问对象 */
@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun remove(songId: Long)

    @Query("SELECT song_id FROM favorites")
    fun observeAll(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavorite(songId: Long): Boolean
}

/** 歌单数据访问对象 */
@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observeById(playlistId: Long): Flow<PlaylistEntity?>
}

@Dao
interface PlayHistoryDao {
    /** 同一首歌只保留最近一次播放时间。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<PlayHistoryEntity>>
}

@Dao
interface SongsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAll(): List<SongEntity>

    @Query("DELETE FROM songs WHERE song_id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("DELETE FROM songs WHERE song_id = :songId")
    suspend fun deleteById(songId: Long)
}
