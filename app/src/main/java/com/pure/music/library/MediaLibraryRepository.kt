package com.pure.music.library

import android.content.Context
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.pure.music.data.Album
import com.pure.music.data.Artist
import com.pure.music.data.Song
import com.pure.music.data.db.AppDatabase
import com.pure.music.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 本地媒体库仓库。MediaStore 负责读取歌曲元数据，ContentObserver 负责感知变化；
 * 刷新请求经过短暂去抖后在 IO 线程查询，再通过 StateFlow 暴露歌曲、专辑和艺术家。
 */
class MediaLibraryRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val songsDao = AppDatabase.get(appContext).songsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

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

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) refresh() else refresh(uri)
        }
    }

    init {
        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        scope.launch {
            val cached = withContext(Dispatchers.IO) { songsDao.getAll().map { it.toSong() } }
            publish(cached)
            refresh()
        }
    }

    /** 请求刷新媒体库；连续触发时只保留最后一次查询。 */
    fun refresh() {
        refresh(null)
    }

    private fun refresh(changedUri: Uri?) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(250)
            val songList = withContext(Dispatchers.IO) {
                if (changedUri != null && changedUri != MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                    val id = changedUri.lastPathSegment?.toLongOrNull()
                    if (id != null) {
                        val song = querySong(id)
                        if (song == null) songsDao.deleteById(id) else songsDao.upsertAll(listOf(song.toEntity()))
                    }
                    songsDao.getAll().map { it.toSong() }
                } else {
                    val scanned = querySongs() ?: return@withContext null
                    syncSongs(scanned)
                    songsDao.getAll().map { it.toSong() }
                }
            }
            songList?.let(::publish)
        }
    }

    private fun publish(songList: List<Song>) {
        _songs.value = songList
        _albums.value = buildAlbums(songList)
        _artists.value = buildArtists(songList)
    }

    private suspend fun syncSongs(scanned: List<Song>) {
        val ids = scanned.map { it.id }
        val existing = songsDao.getAll().associateBy { it.songId }
        val updated = scanned.map { it.toEntity() }.filter { existing[it.songId] != it }
        if (ids.isEmpty()) songsDao.deleteAll() else songsDao.deleteMissing(ids)
        if (updated.isNotEmpty()) songsDao.upsertAll(updated)
    }

    /** 释放资源，取消协程作用域 */
    fun destroy() {
        resolver.unregisterContentObserver(observer)
        scope.cancel()
    }

    /** 从 MediaStore 查询所有音乐文件 */
    private fun querySongs(
        selection: String = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
        selectionArgs: Array<String>? = null
    ): List<Song>? {
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
            ,MediaStore.Audio.Media.DATA
        )
        val results = mutableListOf<Song>()
        val cursor = try {
            resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
            )
        } catch (_: Exception) {
            return null
        } ?: return null
        cursor.use { cursor ->
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
            val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

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
                        ,path = if (pathCol >= 0 && !cursor.isNull(pathCol)) cursor.getString(pathCol) else ""
                    )
                )
            }
        }
        return results
    }

    private fun querySong(id: Long): Song? {
        val selection = "${MediaStore.Audio.Media._ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        return querySongs(selection, arrayOf(id.toString()))?.firstOrNull()
    }

    /** 从歌曲列表聚合生成专辑列表 */
    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.albumId }.map { (albumId, group) ->
            Album(
                albumId = albumId,
                name = group.first().album,
                artist = group.first().artist,
                coverArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ),
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

    private fun Song.toEntity() = SongEntity(
        songId = id,
        uri = uri.toString(),
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        size = size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        trackNumber = trackNumber,
        path = path
    )

    private fun SongEntity.toSong() = Song(
        id = songId,
        uri = Uri.parse(uri),
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        size = size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        trackNumber = trackNumber,
        path = path
    )

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
