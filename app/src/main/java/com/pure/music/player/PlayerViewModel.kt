package com.pure.music.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pure.music.data.Song
import kotlinx.coroutines.flow.StateFlow

/** 播放器 ViewModel，将 PlayerManager 的播放控制暴露给 UI 层 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val state: StateFlow<PlaybackState> = PlayerManager.state

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
}

val playerViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        PlayerViewModel(app)
    }
}
