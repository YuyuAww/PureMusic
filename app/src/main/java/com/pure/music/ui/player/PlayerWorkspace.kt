package com.pure.music.ui.player

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.pure.music.data.Song
import com.pure.music.lyric.LyricData
import com.pure.music.lyric.LyricsCodec
import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.visibleText
import com.pure.music.player.EqualizerController
import com.pure.music.player.LyricsTagService
import com.pure.music.player.PlaybackState
import com.pure.music.player.PlayerManager
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderColors
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 播放页弹层高度已由 Miuix OverlayBottomSheet 接管
private enum class PlaybackMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SHUFFLE("随机播放", Icons.Default.Shuffle),
    SEQUENTIAL("顺序播放", Icons.Default.FormatListNumbered),
    LIST_LOOP("列表循环", Icons.Default.Repeat),
    SINGLE_LOOP("单曲循环", Icons.Default.RepeatOne)
}

private fun playbackMode(state: PlaybackState): PlaybackMode = when {
    state.shuffleModeEnabled -> PlaybackMode.SHUFFLE
    state.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.SINGLE_LOOP
    state.repeatMode == Player.REPEAT_MODE_ALL -> PlaybackMode.LIST_LOOP
    else -> PlaybackMode.SEQUENTIAL
}

/**
 * 全屏播放界面（Miuix 风格）。
 *
 * 背景由封面取色（CoverColors）驱动：渐变背景之上，顶/底栏在支持时叠加
 * Miuix textureBlur 动态模糊；不支持时回退纯色/渐变背景。
 */
@Composable
fun PlayerWorkspace(
    state: PlaybackState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onShuffleMode: (Boolean) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayQueueSong: (Song, List<Song>) -> Unit = { _, _ -> },
    onSetSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {}
) {
    val song = state.currentSong ?: return
    var lyricsJumpNonce by remember { mutableIntStateOf(0) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyricsOps by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    LaunchedEffect(lyricsJumpNonce) {
        if (lyricsJumpNonce > 0) pagerState.animateScrollToPage(2)
    }

    // 兜底颜色：当封面取色失败或尚未加载完成时，防止界面纯黑/纯白
    // 一旦加载成功，CoverColors 将完全接管所有 UI 颜色
    val miuixColors = MiuixTheme.colorScheme
    val fallback = CoverColors(
        accent = miuixColors.primary,
        muted = miuixColors.onSurfaceVariantSummary,
        background = miuixColors.background,
        surface = miuixColors.surface
    )

    val context = LocalContext.current
    var colors by remember(song.albumId) { mutableStateOf(fallback) }

    // 异步加载封面提取的颜色
    val darkTheme = isSystemInDarkTheme()
    LaunchedEffect(song.albumId, darkTheme) {
        colors = loadCoverColors(context, song.albumId, fallback, darkTheme)
    }

    // 中间内容区三个页面（横竖屏共用）
    val playerPages: @Composable (Int) -> Unit = { page ->
        when (page) {
            0 -> DetailPage(song, colors, onOpenLyricsOps = { showLyricsOps = true })
            1 -> CoverAndLyricsPage(song, state.position, state.isPlaying, colors) { lyricsJumpNonce++ }
            else -> LyricsPage(song, state.position, state.isPlaying, colors, onSeek = onSeek)
        }
    }
    // 底部播放控制栏（横屏时堆叠在右侧，竖屏时贴底）
    val playerBottomBar: @Composable () -> Unit = {
        PlayerBottomBar(
            state = state,
            colors = colors,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onRepeatMode = onRepeatMode,
            onShuffleMode = onShuffleMode,
            onQueueClick = { showQueue = true },
            onSetSleepTimer = onSetSleepTimer,
            onCancelSleepTimer = onCancelSleepTimer
        )
    }
    // Miuix 动态模糊：运行时能力检测，支持则叠加 textureBlur，否则回退渐变背景
    val blurSupported = remember { isRuntimeShaderSupported() }
    val backdrop = rememberLayerBackdrop()

    // 顶/底栏统一：支持模糊时叠加 Miuix textureBlur，否则透明（渐变背景直接透出）
    fun Modifier.barBackdrop() = if (blurSupported) textureBlur(backdrop, RectangleShape) else this

    BoxWithConstraints {
        // 背景层：封面取色渐变 + 播放中旋转封面，并捕获进 backdrop 供顶/底栏模糊
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd)))
                .layerBackdrop(backdrop)
        ) {
            if (state.isPlaying) {
                AlbumArt(song, Modifier.fillMaxSize())
            }
        }

        // 横屏：左侧为页面内容，右侧为竖屏的 TopBar 与 BottomBar 堆叠
        val isLandscape = maxWidth > maxHeight
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { page -> playerPages(page) }

                // 右侧复用竖屏的 TopBar 和 BottomBar：TopBar 贴顶、BottomBar 贴底
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(colors.background)
                ) {
                    PlayerTopBar(song = song, colors = colors)
                    Spacer(Modifier.weight(1f))
                    playerBottomBar()
                }
            }
        } else {
            // 竖屏：顶栏贴顶、内容居中、底栏贴底；顶/底栏叠加 Miuix 动态模糊
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().statusBarsPadding().barBackdrop()) {
                    PlayerTopBar(song = song, colors = colors)
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page -> playerPages(page) }
                Box(Modifier.fillMaxWidth().navigationBarsPadding().barBackdrop()) {
                    playerBottomBar()
                }
            }
        }
    }

    if (showQueue) QueueSheet(state, onPlayQueueSong) { showQueue = false }
    if (showLyricsOps) LyricsOpsSheet(song) { showLyricsOps = false }
}

// ---------------------------------------------------------
// 1. TopBar 组件 (包含歌名、歌手与操作按钮)
// ---------------------------------------------------------
@Composable
private fun PlayerTopBar(song: Song, colors: CoverColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, top = 5.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = colors.accent,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                song.artist,
                color = colors.accent.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------
// 2. 中间内容组件：歌曲封面 + 迷你歌词窗
// ---------------------------------------------------------
@Composable
private fun CoverAndLyricsPage(song: Song, position: Long, isPlaying: Boolean, colors: CoverColors, onOpenLyrics: () -> Unit) {
    val doc = rememberLyricsDocument(song)
    // 帧级平滑播放位置：与全屏歌词页同源，上一句/当前句/下一句的切换时刻完全一致
    val smoothPos = rememberSmoothPosition(position, isPlaying, song.id)
    val lines = doc.original
    val currentIndex = lines.indexOfLast { it.startMs?.let { ms -> ms <= smoothPos } ?: false }
    val previous = lines.getOrNull(currentIndex - 1)?.visibleText().orEmpty()
    val current = lines.getOrNull(currentIndex)
    val next = lines.getOrNull(currentIndex + 1)?.visibleText().orEmpty()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AlbumArt(
                song,
                Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenLyrics)
            )
            Spacer(Modifier.height(28.dp))
            MiniLyricsWindow(previous, current, next, smoothPos, colors, onOpenLyrics)
        }
    }
}

@Composable
private fun MiniLyricsWindow(
    previous: String,
    current: LyricLine?,
    next: String,
    smoothPos: Long,
    colors: CoverColors,
    onCurrentClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            previous,
            color = colors.muted.copy(alpha = 0.55f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (current != null && current.words.isNotEmpty()) {
            // 有逐字时间轴：与全屏歌词页同款动画（accent 从左往右填充 + 抬升）
            WordLevelLine(
                current,
                true,
                smoothPos,
                colors,
                Modifier
                    .fillMaxWidth()
                    .clickable { onCurrentClick() }
                    .padding(vertical = 6.dp),
                fontSize = 14.sp,
                alignCenter = false
            )
        } else {
            Text(
                current?.visibleText().orEmpty(),
                color = colors.accent,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onCurrentClick() }
                    .padding(vertical = 6.dp)
            )
        }
        Text(
            next,
            color = colors.muted.copy(alpha = 0.75f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

// ---------------------------------------------------------
// 3. BottomBar 组件 (包含进度条、时间、播放键、工具栏)
// ---------------------------------------------------------
@Composable
private fun PlayerBottomBar(
    state: PlaybackState,
    colors: CoverColors,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onShuffleMode: (Boolean) -> Unit,
    onQueueClick: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var equalizerEnabled by remember { mutableStateOf(EqualizerController.isEnabled) }
    var bandLevels by remember { mutableStateOf(EqualizerController.bandLevels) }

    // Miuix Slider（值驱动 + 拖拽中不回写播放位置）
    var seekValue by remember(state.currentSong?.id, state.duration) {
        mutableFloatStateOf(state.position.toFloat())
    }
    var seekDragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.position, seekDragging) {
        if (!seekDragging) seekValue = state.position.toFloat()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(start = 25.dp, end = 25.dp, top = 5.dp, bottom = 15.dp)
    ) {
        Slider(
            value = seekValue,
            onValueChange = { seekDragging = true; seekValue = it },
            onValueChangeFinished = { seekDragging = false; onSeek(seekValue.toLong()) },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
            colors = accentSliderColors(colors.accent)
        )

        // 时间
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(seekValue.toLong()),
                color = colors.accent,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                formatDuration(state.duration),
                color = colors.accent,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(15.dp))

        // 播放控制区
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(35.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    "上一首",
                    tint = colors.accent,
                    modifier = Modifier.size(35.dp)
                )
            }
            Spacer(Modifier.width(70.dp))
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(45.dp)) {
                Icon(
                    if (state.isPlaying) MiuixIcons.Pause else MiuixIcons.Play,
                    "播放",
                    tint = colors.accent,
                    modifier = Modifier.size(45.dp)
                )
            }
            Spacer(Modifier.width(70.dp))
            IconButton(onClick = onNext, modifier = Modifier.size(35.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    "下一首",
                    tint = colors.accent,
                    modifier = Modifier.size(35.dp)
                )
            }
        }

        Spacer(Modifier.height(45.dp))

        // 底部工具栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mode = playbackMode(state)
            IconButton(onClick = {
                when (mode) {
                    PlaybackMode.SHUFFLE -> {
                        onShuffleMode(false); onRepeatMode(Player.REPEAT_MODE_OFF)
                    }
                    PlaybackMode.SEQUENTIAL -> onRepeatMode(Player.REPEAT_MODE_ALL)
                    PlaybackMode.LIST_LOOP -> onRepeatMode(Player.REPEAT_MODE_ONE)
                    PlaybackMode.SINGLE_LOOP -> {
                        onRepeatMode(Player.REPEAT_MODE_OFF); onShuffleMode(true)
                    }
                }
            }) {
                Icon(mode.icon, mode.label, tint = colors.accent, modifier = Modifier.size(25.dp))
            }
            IconButton(onClick = { showSleepTimer = true }) {
                Icon(
                    MiuixIcons.Timer,
                    "睡眠定时",
                    tint = if (state.sleepMinutes > 0) MiuixTheme.colorScheme.primary else colors.accent,
                    modifier = Modifier.size(25.dp)
                )
            }
            IconButton(onClick = { showEqualizer = true }) {
                Icon(MiuixIcons.Tune, "均衡器", tint = colors.accent, modifier = Modifier.size(25.dp))
            }
            IconButton(onClick = onQueueClick) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    "播放列表",
                    tint = colors.accent,
                    modifier = Modifier.size(25.dp)
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreHoriz, "更多", tint = colors.accent, modifier = Modifier.size(25.dp))
            }
        }
    }

    // 睡眠定时（Miuix 底部弹层 + keyPoints 磁吸到 5 分钟档位）
    var sleepSelection by remember {
        mutableFloatStateOf(state.sleepMinutes.takeIf { it > 0 }?.toFloat() ?: 5f)
    }
    OverlayBottomSheet(
        show = showSleepTimer,
        title = "睡眠定时",
        onDismissRequest = { showSleepTimer = false },
        startAction = {
            IconButton(onClick = { showSleepTimer = false }) { Icon(MiuixIcons.Close, "关闭") }
        },
        endAction = {
            TextButton(
                text = "开始",
                onClick = {
                    onSetSleepTimer(sleepSelection.toInt().coerceIn(5, 60))
                    showSleepTimer = false
                }
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                if (state.sleepMinutes > 0) "剩余 ${state.sleepMinutes} 分钟" else "设置自动暂停时间",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Slider(
                value = sleepSelection,
                onValueChange = { sleepSelection = it },
                valueRange = 5f..60f,
                keyPoints = (5..60 step 5).map { it.toFloat() },
                magnetThreshold = 0.45f,
                colors = accentSliderColors(MiuixTheme.colorScheme.primary)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("5 分钟", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("${sleepSelection.toInt()} 分钟", fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
                Text("60 分钟", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            TextButton(
                text = if (state.sleepMinutes > 0) "取消定时" else "关闭",
                onClick = {
                    onCancelSleepTimer()
                    showSleepTimer = false
                },
                modifier = Modifier.padding(start = 0.dp, top = 16.dp, bottom = 24.dp)
            )
        }
    }

    // DSP 均衡器（Miuix 底部弹层 + 旋转 Miuix Slider 呈现竖直滑块）
    if (showEqualizer) {
        OverlayBottomSheet(
            show = showEqualizer,
            title = "DSP 均衡器",
            onDismissRequest = { showEqualizer = false },
            startAction = {
                IconButton(onClick = { showEqualizer = false }) { Icon(MiuixIcons.Close, "关闭") }
            },
            endAction = {
                Switch(
                    checked = equalizerEnabled,
                    onCheckedChange = { equalizerEnabled = it; EqualizerController.setEnabled(it) },
                    enabled = EqualizerController.isAvailable
                )
            }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bandLevels.forEachIndexed { index, level ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 横排 Miuix Slider 按 180x42 布局后旋转 270° 呈现为竖直滑块，
                            // 外层 Box 预留 42x180 的占位，避免旋转后的绘制压到频段标签
                            Box(Modifier.size(42.dp, 180.dp)) {
                                Slider(
                                    value = if (equalizerEnabled) level else 0f,
                                    onValueChange = { value ->
                                        bandLevels = bandLevels.toMutableList().also { it[index] = value }
                                        EqualizerController.setBandLevel(index, value)
                                    },
                                    valueRange = -1f..1f,
                                    colors = accentSliderColors(MiuixTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .size(width = 180.dp, height = 42.dp)
                                        .align(Alignment.Center)
                                        .graphicsLayer { rotationZ = 270f }
                                )
                            }
                            Text(
                                listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")[index],
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
                Text(
                    "调整各频段增益（dB）",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

/** 封面取色主题下的 Miuix Slider 配色 */
@Composable
private fun accentSliderColors(accent: Color): SliderColors =
    SliderDefaults.sliderColors(
        foregroundColor = accent,
        backgroundColor = accent.copy(alpha = 0.22f),
        thumbColor = accent
    )


// ---------------------------------------------------------
// 播放队列弹层（Miuix OverlayBottomSheet）
// ---------------------------------------------------------
@Composable
private fun QueueSheet(
    state: PlaybackState,
    onPlayQueueSong: (Song, List<Song>) -> Unit,
    onDismiss: () -> Unit
) {
    OverlayBottomSheet(
        show = true,
        title = "播放队列",
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) { Icon(MiuixIcons.Close, "关闭") }
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "${state.queue.size} 首歌曲",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(state.queue, key = { _, item -> item.id }) { index, item ->
                    QueueSongRow(item, index == state.queueIndex) {
                        onPlayQueueSong(item, state.queue)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) colors.primaryContainer else colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(song, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
            Text(
                song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = colors.onSurfaceVariantSummary
            )
        }
        if (isCurrent) {
            Icon(
                MiuixIcons.Tune,
                "正在播放",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// ---------------------------------------------------------
// 辅助页面 (详情页 / 占位歌词)
// ---------------------------------------------------------
/**
 * 歌词解析结果缓存：解析只随歌词原文变化执行一次，播放进度（position）
 * 高频变化时不再重复解析整篇歌词（原实现每次重组都重跑正则）。
 */
@Composable
private fun rememberLyricsDocument(song: Song): LyricsDocument = remember(song.id, song.lyrics) {
    LyricData.of(song.id, song.lyrics).document
        ?: LyricsDocument(original = listOf(LyricLine(text = "暂无内嵌歌词")))
}

/**
 * 帧级平滑播放位置：播放时每 16ms 直接读取播放器真实位置（零漂移、seek 即时生效）；
 * 暂停时钉在真实位置；暂停中 seek 由粗位置的 onPositionDiscontinuity 更新驱动重新钉位。
 */
@Composable
private fun rememberSmoothPosition(position: Long, isPlaying: Boolean, key: Any): Long {
    val value = remember(key) { mutableStateOf(position) }
    LaunchedEffect(position, isPlaying, key) {
        if (!isPlaying) value.value = PlayerManager.currentPositionMs()
    }
    LaunchedEffect(isPlaying, key) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            value.value = PlayerManager.currentPositionMs()
            delay(16)
        }
    }
    return value.value
}

/** 将声道数转为可读标签 */
private fun Int.toChannelLabel(): String = when (this) {
    1 -> "单声道"
    2 -> "双声道"
    in 3..6 -> "$this 声道"
    else -> "$this 声道"
}

@Composable
private fun DetailPage(song: Song, colors: CoverColors, onOpenLyricsOps: () -> Unit) {
    val doc = rememberLyricsDocument(song)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 25.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 歌曲基本信息
        InfoCard("歌曲", colors) {
            DetailRow("标题", song.title, colors)
            DetailRow("艺术家", song.artist, colors)
            DetailRow("专辑", song.album, colors)
            DetailRow("副标题", song.subtitle ?: "", colors)
            DetailRow("专辑艺术家", song.albumArtist ?: "", colors)
        }

        // 歌词（内嵌标签）
        InfoCard("歌词", colors) {
            DetailRow("来源", "内嵌标签", colors)
            DetailRow("格式", doc.format?.label() ?: "纯文本", colors)
            DetailRow("行数", "${doc.original.size} 行", colors)
            DetailRow("逐字", if (doc.hasWordTiming) "支持" else "无逐字时间轴", colors)
            TextButton(
                text = "歌词选项（导入 / 偏移 / 转换 / 写入标签）",
                onClick = onOpenLyricsOps,
                enabled = song.lyrics.orEmpty().isNotBlank() || song.path.isNotBlank()
            )
        }

        // 基本标签
        val basicTags = listOf(
            "曲目" to song.trackNumber.takeIf { it > 0 }?.toString().orEmpty(),
            "碟片" to song.discNumber?.takeIf { it > 0 }?.toString().orEmpty(),
            "年份" to song.year?.toString().orEmpty(),
            "日期" to song.date.orEmpty(),
            "流派" to song.genre.orEmpty(),
            "评论" to song.comment.orEmpty()
        ).filter { it.second.isNotBlank() }
        if (basicTags.isNotEmpty()) InfoCard("基本标签", colors) {
            basicTags.forEach { (k, v) -> DetailRow(k, v, colors) }
        }

        // 扩展标签
        val extTags = listOf(
            "作曲家" to song.composer.orEmpty(),
            "词作者" to song.lyricist.orEmpty(),
            "指挥" to song.conductor.orEmpty(),
            "混音" to song.remixer.orEmpty(),
            "情绪" to song.mood.orEmpty(),
            "BPM" to song.bpm.orEmpty(),
            "ISRC" to song.isrc.orEmpty(),
            "版权" to song.copyright.orEmpty(),
            "厂牌" to song.label.orEmpty()
        ).filter { it.second.isNotBlank() }
        if (extTags.isNotEmpty()) InfoCard("扩展标签", colors) {
            extTags.forEach { (k, v) -> DetailRow(k, v, colors) }
        }

        // MusicBrainz 标识
        val mbTags = listOf(
            "MusicBrainz 曲" to song.musicBrainzTrackId.orEmpty(),
            "MusicBrainz 专辑" to song.musicBrainzAlbumId.orEmpty(),
            "MusicBrainz 艺术家" to song.musicBrainzArtistId.orEmpty()
        ).filter { it.second.isNotBlank() }
        if (mbTags.isNotEmpty()) InfoCard("MusicBrainz", colors) {
            mbTags.forEach { (k, v) -> DetailRow(k, v, colors) }
        }

        // 音频技术参数
        InfoCard("音频信息", colors) {
            DetailRow("格式", song.format?.let { "$it 音频流" } ?: "音频流", colors)
            DetailRow("码率", song.bitrateKbps?.let { "$it kbps" } ?: "", colors)
            DetailRow("采样率", song.sampleRateHz?.let { "$it Hz" } ?: "", colors)
            DetailRow("声道", song.channels?.let { it.toChannelLabel() } ?: "", colors)
            DetailRow("时长", formatDuration(song.duration), colors)
        }
    }
}


@Composable
private fun DetailRow(key: String, value: String, colors: CoverColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            key,
            color = colors.muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(72.dp)
        )
        if (value.isNotBlank()) {
            Text(
                value,
                color = colors.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text("—", color = colors.muted.copy(alpha = 0.4f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, colors: CoverColors, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = 0.7f))
            .padding(24.dp)
    ) {
        Text(
            title,
            color = colors.accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}


/** 歌词列表滚动来源：区分用户浏览与播放自动跟随，只有用户滚动停下才触发跳转 */
private enum class ScrollSource { Idle, User, Programmatic }

@Composable
private fun LyricsPage(song: Song, position: Long, isPlaying: Boolean, colors: CoverColors, onSeek: (Long) -> Unit) {
    val doc = rememberLyricsDocument(song)
    val lines = doc.original
    // 帧级平滑播放位置：粗位置每 500ms 才更新一次，逐字进度需要逐帧连续
    val smoothPos = rememberSmoothPosition(position, isPlaying, song.id)
    val currentIndex = lines.indexOfLast { it.startMs?.let { ms -> ms <= smoothPos } ?: false }
    val listState = rememberLazyListState()
    // 滚动来源状态机：用户 fling/拖拽停下时，seek 到列表垂直中心处的歌词行；
    // 当前行自动跟随的滚动不触发 seek，且用户浏览期间挂起自动跟随，避免播放把列表拽回
    val scrollSource = remember { mutableStateOf(ScrollSource.Idle) }
    val isScrolling = androidx.compose.runtime.derivedStateOf { listState.isScrollInProgress }.value
    fun seekToCenteredLine() {
        val info = listState.layoutInfo
        val centerY = (info.viewportStartOffset + info.viewportEndOffset) / 2
        val centered = info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - centerY) } ?: return
        // 与当前行相同则不重复跳转（避免误触重启）；无时轴行不可跳
        if (centered.index != currentIndex) lines[centered.index].startMs?.let { onSeek(it) }
    }
    LaunchedEffect(song.id, currentIndex) {
        if (currentIndex >= 0 && !listState.isScrollInProgress) {
            scrollSource.value = ScrollSource.Programmatic
            listState.animateScrollToItem(currentIndex)
        }
    }
    LaunchedEffect(isScrolling) {
        when {
            isScrolling -> if (scrollSource.value == ScrollSource.Idle) scrollSource.value = ScrollSource.User
            scrollSource.value == ScrollSource.User -> seekToCenteredLine()
        }
        if (!isScrolling) scrollSource.value = ScrollSource.Idle
    }
    // 音译/翻译轨按行关联键对齐主行
    val romanByKey = remember(doc) { doc.romanization.filter { it.linkKey != null }.groupBy { it.linkKey!! } }
    val transByKey = remember(doc) { doc.translation.filter { it.linkKey != null }.groupBy { it.linkKey!! } }
    Column(Modifier.fillMaxSize().padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 180.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val roman = romanByKey[line.linkKey]?.firstOrNull()?.text
                val translation = transByKey[line.linkKey]?.firstOrNull()?.text
                val isCurrent = index == currentIndex
                val seekToLine: () -> Unit = { line.startMs?.let(onSeek) }
                roman?.let {
                    Text(
                        it,
                        color = colors.muted.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (line.words.isEmpty()) {
                    Text(
                        line.visibleText(),
                        color = if (isCurrent) colors.accent else colors.muted.copy(alpha = 0.45f),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .clickable(enabled = line.startMs != null) { seekToLine() }
                    )
                } else {
                    WordLevelLine(
                        line,
                        isCurrent,
                        smoothPos,
                        colors,
                        Modifier
                            .padding(vertical = 14.dp)
                            .clickable(enabled = line.startMs != null) { seekToLine() }
                    )
                }
                translation?.let {
                    Text(
                        it,
                        color = colors.muted.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "词",
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                doc.format?.label() ?: "纯文本",
                color = colors.muted,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}


/**
 * 逐字歌词行（全屏歌词页与迷你歌词窗共用）：进行中的词由 accent 从左往右
 * 随词内进度填充（硬边扫过）；已唱词全词填充并保持抬升不回落；
 * 未唱词基线下方、底色。超宽时 FlowRow 自动换行、逐行居中，不挤压末尾。
 */
@Composable
private fun WordLevelLine(
    line: LyricLine,
    isCurrent: Boolean,
    smoothPos: Long,
    colors: CoverColors,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    alignCenter: Boolean = true
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = if (alignCenter) Arrangement.Center else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        line.words.forEachIndexed { wordIndex, word ->
            // 逐字匹配：词区间 [wordStart, wordEnd)。LRC 系格式词无 end 时退回"下一词起点"；
            // 末词用 LrcTime 缺省 +500ms 时长收口，不再永久 active（TTML 词起点缺失时同样兜底）
            val wordStart = word.startMs ?: line.startMs ?: 0L
            val nextStart = line.words.getOrNull(wordIndex + 1)?.startMs
            val wordEnd = com.pure.music.lyric.format.LrcTime.wordEndMs(word, nextStart) ?: wordStart + com.pure.music.lyric.format.LrcTime.DEFAULT_WORD_DURATION_MS
            val active = isCurrent && smoothPos in wordStart until wordEnd
            val sung = smoothPos >= wordEnd
            // 唱完后字词保持在抬升位不再回落；历史行已唱词以更低透明度
            // 与当前行区分层次，整行不回到基线下方
            val progress = if (active) {
                ((smoothPos - wordStart).toFloat() / (wordEnd - wordStart).coerceAtLeast(1L)).coerceIn(0f, 1f)
            } else 0f
            val targetLift = when {
                active -> -8f - progress * 5f
                sung -> -8f
                else -> 4f
            }
            val targetAlpha = when {
                active -> 1f
                sung -> if (isCurrent) 0.86f else 0.5f
                else -> 0.48f
            }
            val animation = tween<Float>(180, easing = FastOutSlowInEasing)
            val lift by animateFloatAsState(targetLift, animation, label = "word-lift")
            val alpha by animateFloatAsState(targetAlpha, animation, label = "word-alpha")
            // 高光填充：进行中的词由 accent 从左往右随词内进度扫过（硬边），
            // 未扫到的部分保持底色；已唱词全词填充，未唱词底色
            val wordStyle = when {
                active -> androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    brush = Brush.linearGradient(
                        0f to colors.accent,
                        progress to colors.accent,
                        progress to colors.muted,
                        1f to colors.muted,
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
                sung -> androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
                else -> androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colors.muted
                )
            }
            Text(
                word.text,
                style = wordStyle,
                modifier = Modifier.graphicsLayer { translationY = lift; this.alpha = alpha }
            )
        }
    }
}


/** 歌词操作面板（Miuix 底部弹层）：导入外部歌词文件、时间轴偏移、格式转换，结果写回音频内嵌标签 */
@Composable
private fun LyricsOpsSheet(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricData = remember(song.id, song.lyrics) { LyricData.of(song.id, song.lyrics) }
    val raw = lyricData.rawText.orEmpty()
    val doc = lyricData.document
    val hasWordTiming = lyricData.hasWordTiming
    var offsetMs by remember { mutableLongStateOf(0L) }
    var targetFormat by remember { mutableStateOf(LyricFormat.ENHANCED_LRC) }
    var busy by remember { mutableStateOf(false) }
    val scheme = MiuixTheme.colorScheme

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    /** 写回标签：成功刷新媒体库（歌曲数据经 StateFlow 回流到 UI） */
    fun write(text: String) {
        if (busy) return
        busy = true
        scope.launch {
            val error = LyricsTagService.writeLyrics(context, song, text)
            busy = false
            if (error == null) {
                toast("歌词已写入标签")
                onDismiss()
            } else toast(error)
        }
    }

    // SAF 导入外部歌词文件（.lrc / .ttml / .txt 等）
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            if (text.isNullOrBlank()) toast("无法读取所选歌词文件") else write(text)
        }
    }

    OverlayBottomSheet(
        show = true,
        title = "歌词选项",
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) { Icon(MiuixIcons.Close, "关闭") }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                when {
                    raw.isBlank() -> "当前歌曲暂无内嵌歌词，可导入外部歌词文件写入"
                    doc?.hasWordTiming == true -> "当前为逐字歌词（${doc.format?.label() ?: "格式未知"}），支持转换与偏移"
                    else -> "当前为行级歌词（${doc?.format?.label() ?: "纯文本"}），无逐字时间轴"
                },
                color = scheme.onSurfaceVariantSummary,
                fontSize = 14.sp
            )

            // 导入外部歌词
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("导入外部歌词文件", fontSize = 15.sp, modifier = Modifier.weight(1f))
                TextButton(text = "导入", onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }, enabled = !busy)
            }

            // 时间轴偏移（-10s ~ +10s，步进 100ms，与 Lyrico 一致）
            if (raw.isNotBlank()) {
                Text("时间轴偏移", fontSize = 15.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(text = "-100ms", onClick = { offsetMs = (offsetMs - 100L).coerceAtLeast(-10_000L) })
                    Text("${offsetMs}ms", modifier = Modifier.weight(1f), fontSize = 14.sp, color = scheme.onSurfaceVariantSummary)
                    TextButton(text = "+100ms", onClick = { offsetMs = (offsetMs + 100L).coerceAtMost(10_000L) })
                    TextButton(text = "重置", onClick = { offsetMs = 0L }, enabled = offsetMs != 0L)
                }
                TextButton(
                    text = "应用偏移并写入",
                    onClick = { write(LyricsCodec.shiftText(raw, offsetMs)) },
                    enabled = offsetMs != 0L && !busy
                )
            }
            // 格式转换（Miuix 文本按钮组替代 Material FilterChip）
            if (raw.isNotBlank()) {
                Text("转换歌词格式", fontSize = 15.sp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LyricFormat.entries.forEach { format ->
                        val enabled = !format.usesWordTiming || hasWordTiming
                        MiuixFormatChip(format.label(), targetFormat == format, enabled) { targetFormat = format }
                    }
                }
                Text(
                    if (targetFormat.usesWordTiming && !hasWordTiming) {
                        "源歌词没有逐字时间轴，无法转换到" + targetFormat.label()
                    } else "将转换为 " + targetFormat.label() + " 并写入标签",
                    color = scheme.onSurfaceVariantSummary,
                    fontSize = 12.sp
                )
                TextButton(
                    text = "转换并写入",
                    onClick = {
                        val encoded = doc?.let { LyricsCodec.encode(it, targetFormat) }
                        when {
                            encoded == null -> toast("转换失败：源歌词没有逐字时间轴或解析失败")
                            else -> write(encoded)
                        }
                    },
                    enabled = (targetFormat.usesWordTiming && hasWordTiming) || !targetFormat.usesWordTiming
                )
            }
        }
    }
}

/** 格式选择小按钮（Miuix 胶囊按钮） */
@Composable
private fun MiuixFormatChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MiuixTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        cornerRadius = 14.dp,
        colors = remember(selected) {
            ButtonColors(
                color = if (selected) scheme.primaryContainer else scheme.surfaceContainer,
                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                disabledColor = scheme.disabledPrimary,
                disabledContentColor = scheme.disabledOnSurface,
            )
        },
        insideMargin = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp)
    }
}


