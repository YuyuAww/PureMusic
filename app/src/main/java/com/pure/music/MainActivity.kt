package com.pure.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Song
import com.pure.music.player.PlayerViewModel
import com.pure.music.player.playerViewModelFactory
import com.pure.music.settings.SettingsViewModel
import com.pure.music.settings.settingsViewModelFactory
import com.pure.music.ui.library.LibraryScreen
import com.pure.music.ui.player.MiniPlayerBar
import com.pure.music.ui.player.PlayerWorkspace
import com.pure.music.ui.settings.SettingsScreen
import com.pure.music.ui.theme.PureMusicTheme

/** 主界面 Activity，承载所有 Compose UI */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
    }
}

/** 根 Composable，根据设置切换主题 */
@Composable
private fun MainContent() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()

    // 根据主题设置决定明暗模式
    val darkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // 使用应用品牌色，避免设备动态取色导致界面风格不一致
    PureMusicTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainUI(settingsViewModel)
        }
    }
}

/** 主 UI 层，组装媒体库、播放器、设置等界面 */
@Composable
private fun MainUI(settingsViewModel: SettingsViewModel) {
    val playerViewModel: PlayerViewModel = viewModel(factory = playerViewModelFactory)
    val state by playerViewModel.state.collectAsStateWithLifecycle()
    var showNowPlaying by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showNowPlaying || showSettings) {
        if (showSettings) showSettings = false else showNowPlaying = false
    }

    Scaffold(
        bottomBar = {
            // 底部迷你播放器，有歌曲时显示
            if (state.currentSong != null && !showNowPlaying) {
                MiniPlayerBar(
                    state = state,
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onExpand = { showNowPlaying = true }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showSettings) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { showSettings = false }
                )
            } else {
                LibraryScreen(
                    onPlaySong = { song: Song, queue: List<Song> ->
                        playerViewModel.playSong(song, queue)
                        showNowPlaying = true
                    },
                    onShowSettings = { showSettings = true }
                )

                // 全屏正在播放界面
                if (showNowPlaying) {
                    PlayerWorkspace(
                        state = state,
                        onDismiss = { showNowPlaying = false },
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.next() },
                        onPrevious = { playerViewModel.previous() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onRepeatMode = { playerViewModel.setRepeatMode(it) },
                        onShuffleMode = { playerViewModel.setShuffleMode(it) },
                        isFavorite = playerViewModel.isFavorite(state.currentSong?.id ?: -1L),
                        onToggleFavorite = { playerViewModel.toggleFavorite(state.currentSong?.id ?: -1L) },
                        onPlayFromQueue = { playerViewModel.playQueue(state.queue, it) },
                    )
                }
            }
        }
    }
}
