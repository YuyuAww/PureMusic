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
    val path: String = "",
    // --- 技术参数（TagLib） ---
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
    val format: String? = null,
    // --- 基本标签 ---
    val lyrics: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val comment: String? = null,
    val year: Int? = null,
    val discNumber: Int? = null,
    val subtitle: String? = null,
    val albumArtist: String? = null,
    // --- 扩展标签 ---
    val lyricist: String? = null,
    val conductor: String? = null,
    val remixer: String? = null,
    val mood: String? = null,
    val bpm: String? = null,
    val isrc: String? = null,
    val copyright: String? = null,
    val label: String? = null,
    val musicBrainzTrackId: String? = null,
    val musicBrainzAlbumId: String? = null,
    val musicBrainzArtistId: String? = null
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
