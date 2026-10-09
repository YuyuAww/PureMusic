package com.pure.music

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Song
import com.pure.music.library.LibraryViewModel
import com.pure.music.library.libraryViewModelFactory
import com.pure.music.player.PlayerViewModel
import com.pure.music.player.playerViewModelFactory
import com.pure.music.settings.SettingsViewModel
import com.pure.music.settings.settingsViewModelFactory
import com.pure.music.ui.library.LibraryScreen
import com.pure.music.ui.library.ScanScreen
import com.pure.music.ui.library.SearchScreen
import com.pure.music.ui.player.MiniPlayerBar
import com.pure.music.ui.player.PlayerWorkspace
import com.pure.music.ui.settings.SettingsScreen
import com.pure.music.ui.theme.PureMusicTheme
import top.yukonga.miuix.kmp.basic.Scaffold

/** 主界面 Activity，承载所有 Miuix Compose UI */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏 Edge-to-Edge：状态栏透明，内容绘制到系统栏后方
        enableEdgeToEdge()
        // 移除导航栏半透明遮罩，让底部栏背景色完全延伸至屏幕底部
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent { MainContent() }
    }
}

/** 根 Composable：Miuix 根主题 + Miuix Scaffold（宿主弹窗层 + 迷你播放器浮动工具栏） */
@Composable
private fun MainContent() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory)
    val playerViewModel: PlayerViewModel = viewModel(factory = playerViewModelFactory)
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val colorSource by settingsViewModel.colorSource.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 应用级 Miuix 主题：明暗与取色来源跟随设置
    PureMusicTheme(
        theme = theme,
        colorSource = colorSource,
        coverAlbumId = playerState.currentSong?.albumId,
    ) {
        // 根 Miuix Scaffold：为 Overlay 弹窗提供 popup host，并承载迷你播放器浮动工具栏
        Scaffold(
            floatingToolbar = {
                if (playerState.currentSong != null && !showNowPlaying) {
                    MiniPlayerBar(
                        state = playerState,
                        onPrevious = { playerViewModel.previous() },
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.next() },
                        onExpand = { showNowPlaying = true },
                        isFavorite = playerViewModel.isFavorite(playerState.currentSong?.id ?: -1L),
                        onToggleFavorite = {
                            playerViewModel.toggleFavorite(playerState.currentSong?.id ?: -1L)
                        },
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                when {
                    showSettings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { showSettings = false },
                    )
                    showSearch -> SearchScreen(
                        onBack = { showSearch = false },
                        onPlaySong = { song: Song, queue: List<Song> ->
                            playerViewModel.playSong(song, queue)
                        },
                        viewModel = libraryViewModel,
                    )
                    showScan -> ScanScreen(
                        onBack = { showScan = false },
                        onScan = {
                            libraryViewModel.refresh()
                            showScan = false
                        },
                        viewModel = settingsViewModel,
                    )
                    else -> LibraryScreen(
                        onPlaySong = { song: Song, queue: List<Song> ->
                            val isCurrentSong = playerState.currentSong?.id == song.id
                            playerViewModel.playSong(song, queue)
                            if (isCurrentSong) showNowPlaying = true
                        },
                        onShowSettings = { showSettings = true },
                        onShowSearch = { showSearch = true },
                        onShowScan = { showScan = true },
                        onExit = { (context as? ComponentActivity)?.finish() },
                        viewModel = libraryViewModel,
                    )
                }

                // 全屏播放器（Miuix Overlay 弹层以根 Scaffold 为宿主）
                if (showNowPlaying) {
                    PlayerWorkspace(
                        state = playerState,
                        onDismiss = { showNowPlaying = false },
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.next() },
                        onPrevious = { playerViewModel.previous() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onRepeatMode = { playerViewModel.setRepeatMode(it) },
                        onShuffleMode = { playerViewModel.setShuffleMode(it) },
                        isFavorite = playerViewModel.isFavorite(playerState.currentSong?.id ?: -1L),
                        onToggleFavorite = {
                            playerViewModel.toggleFavorite(playerState.currentSong?.id ?: -1L)
                        },
                        onPlayQueueSong = { song, queue ->
                            playerViewModel.playQueue(queue, queue.indexOf(song))
                        },
                        onSetSleepTimer = playerViewModel::setSleepTimer,
                        onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                    )
                }
            }
        }
    }
}
