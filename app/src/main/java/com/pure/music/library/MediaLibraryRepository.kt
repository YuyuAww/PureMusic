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
import com.pure.music.settings.SettingsRepository
import com.pure.music.taglib.TagLibMetadataReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

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
            // 无 uri 的全量变更多为系统批量广播，避免反复触发全量扫描相互 cancel，交由手动扫描覆盖
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) return
            refresh(uri)
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
            val settings = SettingsRepository(appContext)
            val songList = withContext(Dispatchers.IO) {
                if (changedUri != null && changedUri != MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                    val id = changedUri.lastPathSegment?.toLongOrNull()
                    if (id != null) {
                        val song = querySong(id)
                        if (song == null) songsDao.deleteById(id) else songsDao.upsertAll(listOf(song.toEntity()))
                    }
                    songsDao.getAll().map { it.toSong() }
                } else {
                    val skipShort = settings.skipShortTracks.first()
                    val blocked = settings.blockedFolders.first()
                    val custom = settings.customFolders.first()
                    val useMediaStore = settings.useMediaStore.first()
                    val scanned = querySongs(
                        skipShort = skipShort,
                        blockedFolders = blocked,
                        customFolders = custom,
                        useMediaStore = useMediaStore
                    ) ?: return@withContext null
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
        selectionArgs: Array<String>? = null,
        skipShort: Boolean = false,
        blockedFolders: Set<String> = emptySet(),
        customFolders: Set<String> = emptySet(),
        useMediaStore: Boolean = true
    ): List<Song>? {
        val results = mutableListOf<Song>()
        val seenPaths = mutableSetOf<String>()
        if (useMediaStore) {
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
                , MediaStore.Audio.Media.DATA
            )
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
                    val path = if (pathCol >= 0 && !cursor.isNull(pathCol)) cursor.getString(pathCol) else ""
                    val durationMs = cursor.getLong(durationCol)
                    if (path.isNotBlank()) seenPaths.add(path)
                    val isBlocked = blockedFolders.any { path.startsWith(it) }
                    if (isBlocked) continue
                    if (skipShort && durationMs < 60_000) continue
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
                            duration = durationMs,
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol) * 1000L,
                            dateModified = cursor.getLong(dateModifiedCol) * 1000L,
                            trackNumber = cursor.getInt(trackCol)
                            , path = path
                        ).let { song ->
                            val metadata = if (song.path.isNotBlank()) TagLibMetadataReader.read(song.path) else null
                            val extension = song.path.substringAfterLast('.', "").uppercase().ifBlank { null }
                            if (metadata == null) song.copy(format = extension) else song.copy(
                                title = metadata.title ?: song.title,
                                artist = metadata.artist ?: song.artist,
                                album = metadata.album ?: song.album,
                                trackNumber = metadata.trackNumber ?: song.trackNumber,
                                duration = metadata.durationMs ?: song.duration,
                                bitrateKbps = metadata.bitrateKbps,
                                sampleRateHz = metadata.sampleRateHz,
                                channels = metadata.channels,
                                format = extension,
                                lyrics = metadata.lyrics,
                                composer = metadata.composer,
                                genre = metadata.genre,
                                comment = metadata.comment,
                                year = metadata.year,
                                date = metadata.date,
                                discNumber = metadata.discNumber,
                                subtitle = metadata.subtitle,
                                albumArtist = metadata.albumArtist,
                                lyricist = metadata.lyricist,
                                conductor = metadata.conductor,
                                remixer = metadata.remixer,
                                mood = metadata.mood,
                                bpm = metadata.bpm,
                                isrc = metadata.isrc,
                                copyright = metadata.copyright,
                                label = metadata.label,
                                musicBrainzTrackId = metadata.musicBrainzTrackId,
                                musicBrainzAlbumId = metadata.musicBrainzAlbumId,
                                musicBrainzArtistId = metadata.musicBrainzArtistId
                            )
                        }
                    )
                }
            }
            // 并集合并：MediaStore 结果 + 自定义文件夹结果（按 path 去重）
            if (customFolders.isNotEmpty()) {
                val extra = querySongsFromCustomFolders(customFolders, skipShort, blockedFolders, seenPaths)
                results.addAll(extra)
            }
        } else {
            val scanned = querySongsFromCustomFolders(customFolders, skipShort, blockedFolders)
            results.addAll(scanned)
        }
        return results
    }

    /** 从自定义文件夹直接扫描文件系统；seenPaths 非空时用于跳过已收录的 path（并集去重） */
    private fun querySongsFromCustomFolders(
        customFolders: Set<String>,
        skipShort: Boolean,
        blockedFolders: Set<String>,
        seenPaths: MutableSet<String> = mutableSetOf()
    ): List<Song> {
        val AUDIO_EXTS = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus", "wma", "ape", "mka", "aac")
        val results = mutableListOf<Song>()
        for (folderPath in customFolders) {
            val root = File(folderPath)
            if (!root.exists()) continue
            val files = root.walkTopDown().filter { it.isFile && it.extension.lowercase() in AUDIO_EXTS }.toList()
            for (file in files) {
                val path = file.absolutePath
                if (path in seenPaths) continue
                seenPaths.add(path)
                if (blockedFolders.any { path.startsWith(it) }) continue
                val extension = path.substringAfterLast('.', "").uppercase().ifBlank { null }
                val metadata = TagLibMetadataReader.read(path)
                val durationMs = metadata?.durationMs ?: 0L
                if (skipShort && durationMs < 60_000) continue
                val id = path.hashCode().toLong()
                val uri = Uri.fromFile(file)
                val song = Song(
                    id = id,
                    uri = uri,
                    title = metadata?.title ?: file.nameWithoutExtension,
                    artist = metadata?.artist ?: "未知艺术家",
                    album = metadata?.album ?: "未知专辑",
                    albumId = id,
                    duration = durationMs,
                    size = file.length(),
                    dateAdded = file.lastModified(),
                    dateModified = file.lastModified(),
                    trackNumber = metadata?.trackNumber ?: 0,
                    path = path
                ).let { s ->
                    s.copy(
                        bitrateKbps = metadata?.bitrateKbps,
                        sampleRateHz = metadata?.sampleRateHz,
                        channels = metadata?.channels,
                        format = extension,
                        lyrics = metadata?.lyrics,
                        composer = metadata?.composer,
                        genre = metadata?.genre,
                        comment = metadata?.comment,
                        year = metadata?.year,
                        date = metadata?.date,
                        discNumber = metadata?.discNumber,
                        subtitle = metadata?.subtitle,
                        albumArtist = metadata?.albumArtist,
                        lyricist = metadata?.lyricist,
                        conductor = metadata?.conductor,
                        remixer = metadata?.remixer,
                        mood = metadata?.mood,
                        bpm = metadata?.bpm,
                        isrc = metadata?.isrc,
                        copyright = metadata?.copyright,
                        label = metadata?.label,
                        musicBrainzTrackId = metadata?.musicBrainzTrackId,
                        musicBrainzAlbumId = metadata?.musicBrainzAlbumId,
                        musicBrainzArtistId = metadata?.musicBrainzArtistId
                    )
                }
                results.add(song)
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
        path = path,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        channels = channels,
        format = format,
        lyrics = lyrics,
        composer = composer,
        genre = genre,
        comment = comment,
        year = year,
        date = date,
        discNumber = discNumber,
        subtitle = subtitle,
        albumArtist = albumArtist,
        lyricist = lyricist,
        conductor = conductor,
        remixer = remixer,
        mood = mood,
        bpm = bpm,
        isrc = isrc,
        copyright = copyright,
        label = label,
        musicBrainzTrackId = musicBrainzTrackId,
        musicBrainzAlbumId = musicBrainzAlbumId,
        musicBrainzArtistId = musicBrainzArtistId
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
        path = path,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        channels = channels,
        format = format,
        lyrics = lyrics,
        composer = composer,
        genre = genre,
        comment = comment,
        year = year,
        date = date,
        discNumber = discNumber,
        subtitle = subtitle,
        albumArtist = albumArtist,
        lyricist = lyricist,
        conductor = conductor,
        remixer = remixer,
        mood = mood,
        bpm = bpm,
        isrc = isrc,
        copyright = copyright,
        label = label,
        musicBrainzTrackId = musicBrainzTrackId,
        musicBrainzAlbumId = musicBrainzAlbumId,
        musicBrainzArtistId = musicBrainzArtistId
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
