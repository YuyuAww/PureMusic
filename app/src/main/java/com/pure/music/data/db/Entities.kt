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
