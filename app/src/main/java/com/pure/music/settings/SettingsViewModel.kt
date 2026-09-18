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

/** 设置 ViewModel，提供主题切换等设置的响应式访问 */
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

    val enabledMediaSources: StateFlow<Set<String>> = repository.enabledMediaSources
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("local"))

    fun setTheme(value: String) {
        viewModelScope.launch { repository.setTheme(value) }
    }

    fun setColorSource(value: String) {
        viewModelScope.launch { repository.setColorSource(value) }
    }

    fun toggleMediaSource(sourceId: String) {
        viewModelScope.launch {
            val current = repository.enabledMediaSources.value
            val updated = if (current.contains(sourceId)) current - sourceId else current + sourceId
            repository.setEnabledMediaSources(updated)
        }
    }
}

val settingsViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
        SettingsViewModel(app)
    }
}
