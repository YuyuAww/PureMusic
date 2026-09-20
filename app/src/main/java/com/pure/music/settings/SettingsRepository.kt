package com.pure.music.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 设置仓库，基于 DataStore Preferences 读写应用设置 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val SKIP_SHORT_TRACKS = booleanPreferencesKey("skip_short_tracks")
        val BLOCKED_FOLDERS = stringSetPreferencesKey("blocked_folders")
    }

    /** 主题偏好流，值为 "system" / "light" / "dark" */
    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "system"
    }

    val colorSource: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.COLOR_SOURCE] ?: "monet"
    }

    /** 是否跳过 60 秒以下的音频 */
    val skipShortTracks: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SKIP_SHORT_TRACKS] ?: true
    }

    /** 被屏蔽（不扫描）的文件夹集合 */
    val blockedFolders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.BLOCKED_FOLDERS] ?: emptySet()
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }

    suspend fun setColorSource(source: String) {
        context.dataStore.edit { prefs -> prefs[Keys.COLOR_SOURCE] = source }
    }

    suspend fun setSkipShortTracks(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SKIP_SHORT_TRACKS] = enabled }
    }

    suspend fun setBlockedFolders(folders: Set<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.BLOCKED_FOLDERS] = folders }
    }

    suspend fun addBlockedFolder(path: String) {
        context.dataStore.edit { prefs -> prefs[Keys.BLOCKED_FOLDERS] = (prefs[Keys.BLOCKED_FOLDERS] ?: emptySet()) + path }
    }
}
