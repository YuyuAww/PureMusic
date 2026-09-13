package com.pure.music.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pure.music.data.Song
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.pure.music.data.db.AppDatabase
import com.pure.music.data.db.FavoriteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 播放器 ViewModel，将 PlayerManager 的播放控制暴露给 UI 层 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val state: StateFlow<PlaybackState> = PlayerManager.state
    private val favoriteDao = AppDatabase.get(application).favoritesDao()
    val favoriteSongIds = favoriteDao.observeAll().map { it.toSet() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun playQueue(queue: List<Song>, startIndex: Int) {
        PlayerManager.init(getApplication())
        PlayerManager.playQueue(queue, startIndex)
    }

    fun playSong(song: Song, queue: List<Song>) {
        PlayerManager.init(getApplication())
        PlayerManager.playSong(song, queue)
    }
    fun togglePlayPause() = PlayerManager.togglePlayPause()
    fun next() = PlayerManager.next()
    fun previous() = PlayerManager.previous()
    fun seekTo(position: Long) = PlayerManager.seekTo(position)
    fun setRepeatMode(mode: Int) = PlayerManager.setRepeatMode(mode)
    fun setShuffleMode(enabled: Boolean) = PlayerManager.setShuffleMode(enabled)
    fun clearError() = PlayerManager.clearError()
    fun setSleepTimer(minutes: Int) = PlayerManager.setSleepTimer(minutes)
    fun cancelSleepTimer() = PlayerManager.cancelSleepTimer()
    fun isFavorite(songId: Long): Boolean = songId in favoriteSongIds.value
    fun toggleFavorite(songId: Long) {
        if (songId < 0) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (favoriteDao.isFavorite(songId)) favoriteDao.remove(songId) else favoriteDao.add(FavoriteEntity(songId))
        }
    }
}

val playerViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        PlayerViewModel(app)
    }
}
