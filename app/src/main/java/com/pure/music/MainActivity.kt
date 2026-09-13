package com.pure.music

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.NavigationEventInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Song
import com.pure.music.player.PlayerViewModel
import com.pure.music.player.playerViewModelFactory
import com.pure.music.settings.SettingsViewModel
import com.pure.music.settings.settingsViewModelFactory
import com.pure.music.ui.library.LibraryScreen
import com.pure.music.ui.library.SearchScreen
import com.pure.music.ui.player.MiniPlayerBar
import com.pure.music.ui.player.PlayerWorkspace
import com.pure.music.ui.settings.SettingsScreen
import com.pure.music.ui.theme.PureMusicTheme

/** 主界面 Activity，承载所有 Compose UI */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏 Edge-to-Edge：状态栏透明，内容绘制到系统栏后方，
        // 同时设置 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 实现向后兼容
        enableEdgeToEdge()
        // 移除导航栏半透明遮罩，让底部栏背景色完全延伸至屏幕底部
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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
    val colorSource by settingsViewModel.colorSource.collectAsStateWithLifecycle()
    val playerViewModel: PlayerViewModel = viewModel(factory = playerViewModelFactory)
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    // 根据主题设置决定明暗模式
    val darkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // 使用应用品牌色，避免设备动态取色导致界面风格不一致
        PureMusicTheme(darkTheme = darkTheme, dynamicColor = true, colorSource = colorSource, coverAlbumId = playerState.currentSong?.albumId) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainUI(settingsViewModel, playerViewModel)
        }
    }
}

/** 主 UI 层，组装媒体库、播放器、设置等界面 */
@Composable
private fun MainUI(settingsViewModel: SettingsViewModel, playerViewModel: PlayerViewModel) {
    val state by playerViewModel.state.collectAsStateWithLifecycle()
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNowPlaying by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    val backEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = showNowPlaying || showSettings || showSearch,
        onBackCompleted = {
            when {
                showSettings -> showSettings = false
                showSearch -> showSearch = false
                else -> showNowPlaying = false
            }
        }
    )

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 正常内容区，带系统栏 padding
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    start = padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    top = 0.dp,
                    end = padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = 0.dp
                )
            ) {
                if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { showSettings = false }
                    )
                } else if (showSearch) {
                    SearchScreen(
                        onBack = { showSearch = false },
                        onPlaySong = { song: Song, queue: List<Song> ->
                            val isCurrentSong = state.currentSong?.id == song.id
                            playerViewModel.playSong(song, queue)
                            if (isCurrentSong) showNowPlaying = true
                        }
                    )
                } else {
                    LibraryScreen(
                        onPlaySong = { song: Song, queue: List<Song> ->
                            val isCurrentSong = state.currentSong?.id == song.id
                            playerViewModel.playSong(song, queue)
                            if (isCurrentSong) showNowPlaying = true
                        },
                        onShowSettings = { showSettings = true },
                        onShowSearch = { showSearch = true },
                        onExit = { (context as? ComponentActivity)?.finish() },
                        isDarkTheme = theme == "dark" || (theme == "system" && isSystemInDarkTheme()),
                        onToggleTheme = { settingsViewModel.setTheme(if (theme == "dark") "light" else "dark") },
                        onShowEqualizer = { showSettings = true }
                    )
                }
            }

            // 全屏播放器，不经过 padding，延伸至系统栏后方
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
                    onPlayQueueSong = { song, queue -> playerViewModel.playQueue(queue, queue.indexOf(song)) },
                    onSetSleepTimer = playerViewModel::setSleepTimer,
                    onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                )
            }

            if (state.currentSong != null && !showNowPlaying) {
                MiniPlayerBar(
                    state = state,
                    onPrevious = { playerViewModel.previous() },
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onExpand = { showNowPlaying = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}
