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
