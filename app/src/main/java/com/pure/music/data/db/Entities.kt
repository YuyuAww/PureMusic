package com.pure.music.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** 收藏记录，主键为歌曲 ID */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long
)

/** 歌单记录，songIds 以逗号分隔字符串存储 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "song_ids") val songIds: List<Long> = emptyList()
)

/** List<Long> 与逗号分隔字符串的 Room 类型转换器 */
class LongListConverter {
    @TypeConverter
    fun fromList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<Long> =
        value.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toLongOrNull() }
}

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long
)

/** MediaStore 歌曲索引，缓存元数据以便快速启动和增量同步。 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "album_id") val albumId: Long,
    val duration: Long,
    val size: Long,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
    val path: String
)
