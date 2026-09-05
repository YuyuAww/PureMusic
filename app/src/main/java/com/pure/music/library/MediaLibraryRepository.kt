package com.pure.music.library

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.pure.music.data.Album
import com.pure.music.data.Artist
import com.pure.music.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 媒体库仓库，通过 MediaStore 查询本地音频文件。
 * 使用 ContentObserver 监听媒体库变化自动刷新，通过 StateFlow 暴露响应式数据。
 * 使用双检锁单例模式。
 */
class MediaLibraryRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists

    /** 媒体库变化监听器，文件增删改时自动刷新 */
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        refresh()
    }

    /** 触发媒体库重新扫描 */
    fun refresh() {
        scope.launch {
            val songList = withContext(Dispatchers.IO) { querySongs() }
            _songs.value = songList
            _albums.value = buildAlbums(songList)
            _artists.value = buildArtists(songList)
        }
    }

    /** 释放资源，取消协程作用域 */
    fun destroy() {
        resolver.unregisterContentObserver(observer)
        scope.cancel()
    }

    /** 从 MediaStore 查询所有音乐文件 */
    private fun querySongs(): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.TRACK
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val results = mutableListOf<Song>()
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                results.add(
                    Song(
                        id = id,
                        uri = uri,
                        title = cursor.getString(titleCol) ?: "未知标题",
                        artist = cursor.getString(artistCol) ?: "未知艺术家",
                        album = cursor.getString(albumCol) ?: "未知专辑",
                        albumId = cursor.getLong(albumIdCol),
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateAddedCol) * 1000L,
                        dateModified = cursor.getLong(dateModifiedCol) * 1000L,
                        trackNumber = cursor.getInt(trackCol)
                    )
                )
            }
        }
        return results
    }

    /** 从歌曲列表聚合生成专辑列表 */
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.albumId }.map { (albumId, group) ->
            Album(
                albumId = albumId,
                name = group.first().album,
                artist = group.first().artist,
                coverArtUri = null,
                songCount = group.size,
                songIds = group.map { it.id }
            )
        }.sortedBy { it.name }
    }

    /** 从歌曲列表聚合生成艺术家列表 */
    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { it.artist }.map { (name, group) ->
            Artist(
                name = name,
                albumCount = group.distinctBy { it.albumId }.size,
                songCount = group.size
            )
        }.sortedBy { it.name }
    }

    companion object {
        @Volatile
        private var instance: MediaLibraryRepository? = null

        fun get(context: Context): MediaLibraryRepository {
            return instance ?: synchronized(this) {
                instance ?: MediaLibraryRepository(context).also { instance = it }
            }
        }
    }
}
