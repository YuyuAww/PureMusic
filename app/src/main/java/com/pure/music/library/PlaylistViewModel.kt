package com.pure.music.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pure.music.data.db.AppDatabase
import com.pure.music.data.db.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** 歌单 ViewModel，管理歌单的增删改查 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).playlistDao()

    val playlists: StateFlow<List<PlaylistEntity>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(PlaylistEntity(name = name.trim()))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteById(playlistId)
        }
    }

    /** 向歌单添加歌曲，若歌曲已存在则跳过 */
    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.observeById(playlistId).first().let { playlist ->
                if (playlist != null && playlist.songIds.none { it == songId }) {
                    dao.update(playlist.copy(songIds = playlist.songIds + songId))
                }
            }
        }
    }

    /** 从歌单移除歌曲 */
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.observeById(playlistId).first().let { playlist ->
                if (playlist != null) {
                    dao.update(playlist.copy(songIds = playlist.songIds - songId))
                }
            }
        }
    }
}

val playlistViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        PlaylistViewModel(app)
    }
}
