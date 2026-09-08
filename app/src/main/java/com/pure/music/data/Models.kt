package com.pure.music.data

import android.net.Uri

/** 歌曲数据模型，对应 MediaStore 中的一条音频记录 */
data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val trackNumber: Int,
    val path: String = ""
)

/** 专辑数据模型，由歌曲聚合生成 */
data class Album(
    val albumId: Long,
    val name: String,
    val artist: String,
    val coverArtUri: Uri?,
    val songCount: Int,
    val songIds: List<Long>
)

/** 艺术家数据模型，由歌曲聚合生成 */
data class Artist(
    val name: String,
    val albumCount: Int,
    val songCount: Int
)

data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int
)
