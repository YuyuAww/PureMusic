package com.pure.music.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 设置 ViewModel，提供主题切换与媒体扫描配置的响应式访问 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val theme: StateFlow<String> = repository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "system"
        )
    val colorSource: StateFlow<String> = repository.colorSource
        .stateIn(viewModelScope, SharingStarted.Eagerly, "monet")

    val skipShortTracks: StateFlow<Boolean> = repository.skipShortTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val blockedFolders: StateFlow<Set<String>> = repository.blockedFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setTheme(value: String) {
        viewModelScope.launch { repository.setTheme(value) }
    }

    fun setColorSource(value: String) {
        viewModelScope.launch { repository.setColorSource(value) }
    }

    fun setSkipShortTracks(enabled: Boolean) {
        viewModelScope.launch { repository.setSkipShortTracks(enabled) }
    }

    fun setBlockedFolders(folders: Set<String>) {
        viewModelScope.launch { repository.setBlockedFolders(folders) }
    }

    fun addBlockedFolder(path: String) {
        viewModelScope.launch { repository.addBlockedFolder(path) }
    }
}

val settingsViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        SettingsViewModel(app)
    }
}
