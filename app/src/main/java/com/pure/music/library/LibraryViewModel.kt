package com.pure.music.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pure.music.data.Album
import com.pure.music.data.Artist
import com.pure.music.data.MusicFolder
import com.pure.music.data.Song
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.pure.music.data.db.AppDatabase
import com.pure.music.data.db.FavoriteEntity

/** 媒体库 ViewModel，桥接仓库与 UI，暴露歌曲/专辑/艺术家列表 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaLibraryRepository.get(application)
    private val database = AppDatabase.get(application)

    val songs: StateFlow<List<Song>> = repository.songs
    val albums: StateFlow<List<Album>> = repository.albums
    val artists: StateFlow<List<Artist>> = repository.artists

    val favoriteSongIds: StateFlow<Set<Long>> = database.favoritesDao().observeAll()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val recentSongIds: StateFlow<List<Long>> = database.historyDao().observeRecent()
        .map { it.map { item -> item.songId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = songs.map { list ->
        list.mapNotNull { song ->
            song.path.takeIf { it.isNotBlank() }?.let { path ->
                val folderPath = path.substringBeforeLast('/', "未知文件夹")
                MusicFolder(folderPath, folderPath.substringAfterLast('/'), 1)
            }
        }.groupBy { it.path }.map { (path, items) ->
            MusicFolder(path, items.first().name, items.size)
        }.sortedBy { it.path }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun songsInFolder(path: String): List<Song> = songs.value.filter { it.path.substringBeforeLast('/', "") == path }

    fun refresh() = repository.refresh()

    fun toggleFavorite(songId: Long) = viewModelScope.launch {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (favoriteSongIds.value.contains(songId)) database.favoritesDao().remove(songId)
            else database.favoritesDao().add(FavoriteEntity(songId))
        }
    }
}

val libraryViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        LibraryViewModel(app)
    }
}
