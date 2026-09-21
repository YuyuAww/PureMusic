package com.pure.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.Player
import kotlin.math.roundToInt
import com.pure.music.data.Song
import com.pure.music.lyric.LyricsCodec
import com.pure.music.lyric.format.LrcTime
import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.visibleText
import com.pure.music.player.LyricsTagService
import com.pure.music.player.PlayerManager
import com.pure.music.player.PlaybackState
import com.pure.music.player.EqualizerController
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PlayerSheetHeight = 480.dp

@OptIn(ExperimentalMaterial3Api::class)
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

    // 兜底颜色：当封面取色失败或尚未加载完成时，使用系统 Material 颜色防止界面纯黑/纯白
    // 一旦加载成功，CoverColors 将完全接管所有 UI 颜色
    val fallback = CoverColors(
        accent = MaterialTheme.colorScheme.primary,
        muted = MaterialTheme.colorScheme.onSurfaceVariant,
        background = MaterialTheme.colorScheme.background,
        surface = MaterialTheme.colorScheme.surface
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
            else -> LyricsPage(song, state.position, state.isPlaying, colors)
        }
    }
    // 底部播放控制栏（横屏时堆叠在右侧，竖屏时由 Scaffold 承载）
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

    BoxWithConstraints {
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
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(colors.background)
                ) {
                    PlayerTopBar(song = song, colors = colors)
                    Spacer(Modifier.weight(1f))
                    playerBottomBar()
                }
            }
        } else {
            // 竖屏：使用 Scaffold 划分三大组件区域
            Scaffold(
                containerColor = colors.background, // 整体背景：由封面背景色决定
                contentWindowInsets = WindowInsets(0, 0, 0, 0), // 移除默认 Insets，交给子组件自行处理
                topBar = {
                    // 1. TopBar 组件
                    PlayerTopBar(song = song, colors = colors)
                },
                bottomBar = {
                    // 3. BottomBar 组件
                    playerBottomBar()
                }
            ) { paddingValues ->
                // 2. Content (中间内容) 组件
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { page -> playerPages(page) }
            }
        }
    }
    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Column(Modifier.fillMaxWidth().height(PlayerSheetHeight).padding(horizontal = 20.dp)) {
                Text("播放队列", style = MaterialTheme.typography.headlineSmall)
                Text("${state.queue.size} 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(state.queue, key = { _, item -> item.id }) { index, item ->
                        QueueSongRow(item, index == state.queueIndex) {
                            onPlayQueueSong(item, state.queue)
                            showQueue = false
                        }
                    }
                }
            }
        }
    }
    if (showLyricsOps) LyricsOpsSheet(song) { showLyricsOps = false }
}

// ---------------------------------------------------------
// 1. TopBar 组件 (包含歌名、歌手)
// ---------------------------------------------------------
@Composable
private fun PlayerTopBar(song: Song, colors: CoverColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.background) // 与页面主体保持同一背景，避免顶部割裂
            .statusBarsPadding() // 内容避开状态栏
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 15.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = colors.accent, // 封面主色
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                song.artist,
                color = colors.accent.copy(alpha = 0.85f), // 封面主色（淡化）
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
    Column(Modifier.fillMaxSize().padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 歌曲封面
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .shadow(10.dp, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(colors.gradientStart.copy(alpha = .18f), colors.surface))) // 封面渐变色衬底
        ) {
            AlbumArt(song, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(35.dp))
        // 迷你歌词窗：取播放进度之前最近的一行；使用 lastOrNull 避免始终停留在第一行
        val timedLines = doc.original.filter { it.startMs != null }
        val current = timedLines.lastOrNull { it.startMs!! <= smoothPos } ?: doc.original.firstOrNull()
        val index = current?.let { timedLines.indexOf(it) }?.coerceAtLeast(0) ?: -1
        val textOf: (LyricLine?) -> String = { it?.let { line -> line.visibleText() } ?: "" }
        MiniLyricsWindow(
            textOf(timedLines.getOrNull(index - 1)),
            current,
            textOf(timedLines.getOrNull(index + 1)),
            smoothPos,
            colors,
            onOpenLyrics
        )
    }
}

@Composable
private fun MiniLyricsWindow(previous: String, current: LyricLine?, next: String, smoothPos: Long, colors: CoverColors, onCurrentClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(previous, color = colors.muted.copy(alpha = 0.55f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 7.dp))
        if (current != null && current.words.isNotEmpty()) {
            // 有逐字时间轴：与全屏歌词页同款动画（accent 从左往右填充 + 抬升）
            WordLevelLine(current, true, smoothPos, colors, Modifier.fillMaxWidth().clickable { onCurrentClick() }.padding(vertical = 9.dp))
        } else {
            Text(current?.visibleText().orEmpty(), color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onCurrentClick() }.padding(vertical = 9.dp))
        }
        Text(next, color = colors.muted.copy(alpha = 0.75f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 7.dp))
    }
}

// ---------------------------------------------------------
// 3. BottomBar 组件 (包含进度条、时间、播放键、工具栏)
// ---------------------------------------------------------
@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
    var sleepSelection by remember { mutableFloatStateOf(5f) }
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.accent,
        activeTrackColor = colors.accent,
        inactiveTrackColor = colors.accent.copy(alpha = .22f)
    )
    val seekSliderState = remember(state.currentSong?.id, state.duration) {
        SliderState(
            value = state.position.toFloat(),
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat()
        )
    }
    LaunchedEffect(state.position, seekSliderState.isDragging) {
        if (!seekSliderState.isDragging) {
            seekSliderState.value = state.position.toFloat()
        }
    }
    LaunchedEffect(seekSliderState) {
        seekSliderState.onValueChangeFinished = { onSeek(seekSliderState.value.toLong()) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background) // 底部背景：与 TopBar 保持一致
            .navigationBarsPadding() // 内容避开底部导航栏
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 20.dp)
    ) {
        Slider(
            state = seekSliderState,
            colors = sliderColors
        )

        // 时间
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(seekSliderState.value.toLong()), color = colors.accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(formatDuration(state.duration), color = colors.accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))

        // 播放控制区
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipPrevious, "上一首", tint = colors.accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(70.dp))
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(42.dp)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", tint = colors.accent, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.width(70.dp))
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipNext, "下一首", tint = colors.accent, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(44.dp))

        // 底部工具栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mode = playbackMode(state)
            IconButton(onClick = {
                when (mode) {
                    PlaybackMode.SHUFFLE -> { onShuffleMode(false); onRepeatMode(Player.REPEAT_MODE_OFF) }
                    PlaybackMode.SEQUENTIAL -> onRepeatMode(Player.REPEAT_MODE_ALL)
                    PlaybackMode.LIST_LOOP -> onRepeatMode(Player.REPEAT_MODE_ONE)
                    PlaybackMode.SINGLE_LOOP -> { onRepeatMode(Player.REPEAT_MODE_OFF); onShuffleMode(true) }
                }
            }) {
                Icon(mode.icon, mode.label, tint = colors.accent, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = { sleepSelection = state.sleepMinutes.takeIf { it > 0 }?.toFloat() ?: 5f; showSleepTimer = true }) { Icon(Icons.Default.Alarm, "睡眠定时", tint = if (state.sleepMinutes > 0) MaterialTheme.colorScheme.primary else colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = { showEqualizer = true }) { Icon(Icons.Default.GraphicEq, "均衡器", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = onQueueClick) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放列表", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, "更多", tint = colors.accent, modifier = Modifier.size(24.dp)) }
        }
    }
    if (showSleepTimer) {
        ModalBottomSheet(onDismissRequest = { showSleepTimer = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(PlayerSheetHeight)
                    .padding(horizontal = 20.dp)
            ) {
                Text("睡眠定时", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(if (state.sleepMinutes > 0) "剩余 ${state.sleepMinutes} 分钟" else "设置自动暂停时间")
                Spacer(Modifier.height(12.dp))
                val sleepSliderState = remember {
                    SliderState(value = sleepSelection, valueRange = 5f..60f, steps = 10)
                }
                LaunchedEffect(sleepSliderState) {
                    sleepSliderState.onValueChange = { sleepSelection = (it / 5f).roundToInt() * 5f }
                }
                Slider(
                    state = sleepSliderState
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5 分钟")
                    Text("${sleepSelection.toInt()} 分钟")
                    Text("60 分钟")
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onCancelSleepTimer(); showSleepTimer = false }) {
                        Text(if (state.sleepMinutes > 0) "取消定时" else "关闭")
                    }
                    TextButton(onClick = { onSetSleepTimer(sleepSelection.toInt().coerceIn(5, 60)); showSleepTimer = false }) {
                        Text("开始")
                    }
                }
            }
        }
    }
    if (showEqualizer) {
        ModalBottomSheet(onDismissRequest = { showEqualizer = false }) {
            Column(Modifier.fillMaxWidth().height(PlayerSheetHeight).padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DSP 均衡器", style = MaterialTheme.typography.headlineSmall)
                    Switch(
                        checked = equalizerEnabled,
                        enabled = EqualizerController.isAvailable,
                        onCheckedChange = { equalizerEnabled = it; EqualizerController.setEnabled(it) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    bandLevels.forEachIndexed { index, level ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 横排 Slider 按 180x42 布局后旋转 270° 呈现为竖直滑块，
                            // 外层 Box 预留 42x180 的占位，避免旋转后的绘制压到频段标签
                            Box(Modifier.size(42.dp, 180.dp)) {
                                val eqSliderState = remember(index) {
                                    SliderState(value = if (equalizerEnabled) level else 0f, valueRange = -1f..1f)
                                }
                                LaunchedEffect(eqSliderState) {
                                    eqSliderState.onValueChange = { value ->
                                        bandLevels = bandLevels.toMutableList().also { it[index] = value }
                                        EqualizerController.setBandLevel(index, value)
                                    }
                                }
                                Slider(
                                    state = eqSliderState,
                                    modifier = Modifier.size(width = 180.dp, height = 42.dp).align(Alignment.Center).graphicsLayer { rotationZ = 270f }
                                )
                            }
                            Text(listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")[index], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("调整各频段增益（dB）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------
// 辅助页面 (详情页 / 全屏歌词页 / 占位歌词)
// ---------------------------------------------------------
/**
 * 歌词解析结果缓存：解析只随歌词原文变化执行一次，播放进度（position）
 * 高频变化时不再重复解析整篇歌词（原实现每次重组都重跑正则）。
 */
@Composable
private fun rememberLyricsDocument(song: Song): LyricsDocument = remember(song.id, song.lyrics) {
    LyricsCodec.parse(song.lyrics) ?: LyricsDocument(original = listOf(LyricLine(text = "暂无内嵌歌词")))
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


@Composable
private fun LyricsPage(song: Song, position: Long, isPlaying: Boolean, colors: CoverColors) {
    val doc = rememberLyricsDocument(song)
    val lines = doc.original
    // 帧级平滑播放位置：粗位置每 500ms 才更新一次，逐字进度需要逐帧连续
    val smoothPos = rememberSmoothPosition(position, isPlaying, song.id)
    val currentIndex = lines.indexOfLast { it.startMs?.let { ms -> ms <= smoothPos } ?: false }
    val listState = rememberLazyListState()
    LaunchedEffect(song.id, currentIndex) { if (currentIndex >= 0) listState.animateScrollToItem(currentIndex) }
    // 音译/翻译轨按行关联键对齐主行
    val romanByKey = remember(doc) { doc.romanization.filter { it.linkKey != null }.groupBy { it.linkKey!! } }
    val transByKey = remember(doc) { doc.translation.filter { it.linkKey != null }.groupBy { it.linkKey!! } }
    Column(Modifier.fillMaxSize().padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 180.dp)) {
            itemsIndexed(lines) { index, line ->
                val roman = romanByKey[line.linkKey]?.firstOrNull()?.text
                val translation = transByKey[line.linkKey]?.firstOrNull()?.text
                roman?.let { Text(it, color = colors.muted.copy(alpha = .5f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 2.dp)) }
                val isCurrent = index == currentIndex
                if (line.words.isEmpty()) Text(
                    line.visibleText(),
                    color = if (isCurrent) colors.accent else colors.muted.copy(alpha = .45f),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                ) else WordLevelLine(line, isCurrent, smoothPos, colors, Modifier.padding(vertical = 14.dp))
                translation?.let { Text(it, color = colors.muted.copy(alpha = .6f), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("词", color = colors.accent, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(colors.surface.copy(alpha = .7f)).padding(horizontal = 7.dp, vertical = 4.dp))
            Spacer(Modifier.width(10.dp))
            Text(doc.format?.label() ?: "纯文本", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        line.words.forEachIndexed { wordIndex, word ->
            // 逐字匹配：词区间 [wordStart, wordEnd)。LRC 系格式词无 end 时退回"下一词起点"；
            // 末词用 LrcTime 缺省 +500ms 时长收口，不再永久 active（TTML 词起点缺失时同样兜底）
            val wordStart = word.startMs ?: line.startMs ?: 0L
            val nextStart = line.words.getOrNull(wordIndex + 1)?.startMs
            val wordEnd = LrcTime.wordEndMs(word, nextStart) ?: wordStart + LrcTime.DEFAULT_WORD_DURATION_MS
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
                sung -> if (isCurrent) .86f else .5f
                else -> .48f
            }
            val animation = tween<Float>(180, easing = FastOutSlowInEasing)
            val lift by animateFloatAsState(targetLift, animation, label = "word-lift")
            val alpha by animateFloatAsState(targetAlpha, animation, label = "word-alpha")
            // 高光填充：进行中的词由 accent 从左往右随词内进度扫过（硬边），
            // 未扫到的部分保持底色；已唱词全词填充，未唱词底色
            val wordStyle = when {
                active -> TextStyle(
                    fontSize = 21.sp,
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
                sung -> TextStyle(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
                else -> TextStyle(
                    fontSize = 21.sp,
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
            TextButton(onClick = onOpenLyricsOps, enabled = song.lyrics.orEmpty().isNotBlank() || song.path.isNotBlank()) {
                Text("歌词选项（导入 / 偏移 / 转换 / 写入标签）", color = colors.accent)
            }
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

/** 歌词操作面板：导入外部歌词文件、时间轴偏移、格式转换，结果写回音频内嵌标签（Lyrico 同款交互） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsOpsSheet(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val raw = song.lyrics.orEmpty()
    val doc = remember(song.id, song.lyrics) { LyricsCodec.parse(song.lyrics) }
    val hasWordTiming = doc?.hasWordTiming == true
    var offsetMs by remember { mutableLongStateOf(0L) }
    var targetFormat by remember { mutableStateOf(LyricFormat.ENHANCED_LRC) }
    var busy by remember { mutableStateOf(false) }

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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().height(PlayerSheetHeight).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("歌词选项", style = MaterialTheme.typography.headlineSmall)
            Text(
                when {
                    raw.isBlank() -> "当前歌曲暂无内嵌歌词，可导入外部歌词文件写入"
                    doc?.hasWordTiming == true -> "当前为逐字歌词（${doc.format?.label() ?: "格式未知"}），支持转换与偏移"
                    else -> "当前为行级歌词（${doc?.format?.label() ?: "纯文本"}），无逐字时间轴"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            // 导入外部歌词
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("导入外部歌词文件", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }, enabled = !busy) {
                    Text("导入")
                }
            }

            // 时间轴偏移（-10s ~ +10s，步进 100ms，与 Lyrico 一致）
            if (raw.isNotBlank()) {
                Text("时间轴偏移", style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { offsetMs = (offsetMs - 100L).coerceAtLeast(-10_000L) }) { Text("-100ms") }
                    Text("${offsetMs}ms", modifier = Modifier.weight(1f))
                    TextButton(onClick = { offsetMs = (offsetMs + 100L).coerceAtMost(10_000L) }) { Text("+100ms") }
                    TextButton(onClick = { offsetMs = 0L }, enabled = offsetMs != 0L) { Text("重置") }
                }
                TextButton(onClick = { write(LyricsCodec.shiftText(raw, offsetMs)) }, enabled = offsetMs != 0L && !busy) {
                    Text("应用偏移并写入")
                }
            }

            // 格式转换
            if (raw.isNotBlank()) {
                Text("转换歌词格式", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LyricFormat.entries.forEach { format ->
                        val enabled = !format.usesWordTiming || hasWordTiming
                        FilterChip(
                            selected = targetFormat == format,
                            onClick = { targetFormat = format },
                            enabled = enabled,
                            label = { Text(format.label()) }
                        )
                    }
                }
                Text(
                    if (targetFormat.usesWordTiming && !hasWordTiming) {
                        "源歌词没有逐字时间轴，无法转换到" + targetFormat.label()
                    } else "将转换为 " + targetFormat.label() + " 并写入标签",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                TextButton(
                    onClick = {
                        val encoded = doc?.let { LyricsCodec.encode(it, targetFormat) }
                        when {
                            encoded == null -> toast("转换失败：源歌词没有逐字时间轴或解析失败")
                            else -> write(encoded)
                        }
                    },
                    enabled = (targetFormat.usesWordTiming && hasWordTiming) || !targetFormat.usesWordTiming
                ) {
                    Text("转换并写入")
                }
            }
        }
    }
}

/** 将声道数转为可读标签 */
private fun Int.toChannelLabel(): String = when (this) {
    1 -> "单声道"
    2 -> "双声道"
    in 3..6 -> "$this 声道"
    else -> "$this 声道"
}

@Composable
private fun DetailRow(key: String, value: String, colors: CoverColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(key, color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
        if (value.isNotBlank()) {
            Text(value, color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        } else {
            Text("—", color = colors.muted.copy(alpha = 0.4f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun DetailCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, colors: CoverColors) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(20.dp))
        Text(text, color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(title: String, colors: CoverColors, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(24.dp)) {
        Text(title, color = colors.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

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

@Composable
private fun QueueSongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(song, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isCurrent) Icon(Icons.Default.GraphicEq, "正在播放", tint = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.MoreVert, "更多操作")
    }

}
