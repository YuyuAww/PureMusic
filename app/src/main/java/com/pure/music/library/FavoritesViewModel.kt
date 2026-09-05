package com.pure.music.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pure.music.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 收藏 ViewModel，管理歌曲收藏状态 */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).favoritesDao()

    /** 收藏歌曲 ID 集合，响应式更新 */
    val favoriteSongIds: StateFlow<Set<Long>> = dao.observeAll()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun isFavorite(songId: Long): Boolean =
        favoriteSongIds.value.contains(songId)

    /** 切换收藏状态：已收藏则取消，未收藏则添加 */
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isFavorite(songId)) {
                dao.remove(songId)
            } else {
                dao.add(songId)
            }
        }
    }
}

val favoritesViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        FavoritesViewModel(app)
    }
}
