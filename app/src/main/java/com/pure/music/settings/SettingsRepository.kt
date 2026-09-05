package com.pure.music.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 设置仓库，基于 DataStore Preferences 读写应用设置 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
    }

    /** 主题偏好流，值为 "system" / "light" / "dark" */
    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "system"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }
}
