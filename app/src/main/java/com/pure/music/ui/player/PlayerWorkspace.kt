package com.pure.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.roundToInt
import com.pure.music.data.Song
import com.pure.music.player.PlaybackState
import com.pure.music.player.EqualizerController
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors

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
            0 -> DetailPage(song, colors)
            1 -> CoverAndLyricsPage(song, state.position, colors) { lyricsJumpNonce++ }
            else -> LyricsPage(song, state.position, colors)
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
private fun CoverAndLyricsPage(song: Song, position: Long, colors: CoverColors, onOpenLyrics: () -> Unit) {
    val lines = timedLyrics(song)
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
        // 迷你歌词窗
        // 取播放进度之前最近的一行；使用 lastOrNull 避免始终停留在第一行
        val primaryLines = lines.filter { it.primary }
        val current = primaryLines.lastOrNull { it.timeMs <= position } ?: primaryLines.firstOrNull() ?: lines.first()
        val index = primaryLines.indexOf(current).coerceAtLeast(0)
        MiniLyricsWindow(
            primaryLines.getOrNull(index - 1)?.text ?: "",
            current.text,
            primaryLines.getOrNull(index + 1)?.text ?: "",
            colors,
            onOpenLyrics
        )
    }
}

@Composable
private fun MiniLyricsWindow(previous: String, current: String, next: String, colors: CoverColors, onCurrentClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(previous, color = colors.muted.copy(alpha = 0.55f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 7.dp))
        Text(current, color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onCurrentClick() }.padding(vertical = 9.dp))
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
    var sliderPosition by remember(state.currentSong?.id) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var equalizerEnabled by remember { mutableStateOf(EqualizerController.isEnabled) }
    var bandLevels by remember { mutableStateOf(EqualizerController.bandLevels) }
    var sleepSelection by remember { mutableFloatStateOf(5f) }
    LaunchedEffect(state.position, dragging) { if (!dragging) sliderPosition = state.position.toFloat() }
    val displayedPosition = if (dragging) sliderPosition.toLong() else state.position

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background) // 底部背景：与 TopBar 保持一致
            .navigationBarsPadding() // 内容避开底部导航栏
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 20.dp)
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { dragging = true; sliderPosition = it },
            onValueChangeFinished = { dragging = false; onSeek(sliderPosition.toLong()) },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
            interactionSource = sliderInteractionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteractionSource,
                    modifier = Modifier.size(8.dp),
                    colors = SliderDefaults.colors(thumbColor = colors.accent)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderPositions = SliderPositions(
                        initialActiveRange = 0f..((sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.height(2.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.accent.copy(alpha = .22f)
                    )
                )
            }
        )

        // 时间
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(displayedPosition), color = colors.accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
            IconButton(onClick = onQueueClick) { Icon(Icons.Default.QueueMusic, "播放列表", tint = colors.accent, modifier = Modifier.size(24.dp)) }
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
                Slider(
                    value = sleepSelection,
                    onValueChange = { sleepSelection = (it / 5f).roundToInt() * 5f },
                    valueRange = 5f..60f,
                    steps = 10
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
                    Text("均衡器", style = MaterialTheme.typography.headlineSmall)
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
                                Slider(
                                    value = if (equalizerEnabled) level else 0f,
                                    onValueChange = { value -> bandLevels = bandLevels.toMutableList().also { it[index] = value }; EqualizerController.setBandLevel(index, value) },
                                    valueRange = -1f..1f,
                                    modifier = Modifier.size(width = 180.dp, height = 42.dp).align(Alignment.Center).graphicsLayer { rotationZ = 270f }
                                )
                            }
                            Text(listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")[index], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(if (EqualizerController.isAvailable) "调整各频段增益（dB）" else "当前设备不支持系统均衡器", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------
// 辅助页面 (详情页 / 全屏歌词页 / 占位歌词)
// ---------------------------------------------------------
private data class TimedLyric(val timeMs: Long, val text: String, val words: List<TimedWord> = emptyList(), val primary: Boolean = false, val roma: String? = null, val translation: String? = null)
private data class TimedWord(val timeMs: Long, val text: String)

private fun timedLyrics(song: Song): List<TimedLyric> {
    val source = song.lyrics.orEmpty()
    if (source.trimStart().startsWith("<tt", ignoreCase = true)) return parseTtmlLyrics(source)
    val regex = Regex("([<\\[])(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?[>\\]]")
    val parsed = source.lines().flatMap { line ->
        val matches = regex.findAll(line).toList()
        if (matches.isEmpty()) listOf(TimedLyric(0, line.trim())) else {
            val lineStart = matches.firstOrNull { it.value.startsWith("[") }
            // 实际本地歌词同时存在两种增强 LRC 写法：<mm:ss.xx> 和 [mm:ss.xx]。
            // 当同一行包含多个时间标记时，第一个是行时间，后续标记是逐字时间。
            val wordMatches = if (matches.size > 1) matches.drop(1) else emptyList()
            val words = wordMatches.mapIndexed { index, m ->
                val fraction = m.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0
                val time = (m.groupValues[2].toLong() * 60 + m.groupValues[3].toLong()) * 1000 + fraction
                val end = wordMatches.getOrNull(index + 1)?.range?.first ?: line.length
                TimedWord(time, line.substring(m.range.last + 1, end))
            }.filter { it.text.isNotBlank() }
            val text = if (words.isNotEmpty()) words.map { it.text.trim() }.reduce { a, b ->
                val separator = if (a.lastOrNull()?.isLatinOrDigit() == true && b.firstOrNull()?.isLatinOrDigit() == true) " " else ""
                a + separator + b
            } else line.substringAfter("]", line).trim()
            listOf(TimedLyric(lineStart?.let(::parseLyricTime) ?: parseLyricTime(matches.first()), text, words, lineStart != null))
        }
    }.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }
    val merged = parsed.groupBy { it.timeMs }.toSortedMap().values.map { group ->
        val main = group.firstOrNull { it.words.isNotEmpty() } ?: group.firstOrNull { it.primary }
        val secondary = group.filter { it !== main }
        val roma = secondary.firstOrNull { it.text.isMostlyAscii() }?.text
        val translation = secondary.firstOrNull { !it.text.isMostlyAscii() }?.text
        main?.copy(roma = roma, translation = translation) ?: group.first()
    }
    return merged.ifEmpty { listOf(TimedLyric(0, "暂无内嵌歌词")) }
}

/** 兼容 Lyrico/TTML：主行使用 span begin，翻译和罗马音使用 role 标记。 */
private fun parseTtmlLyrics(source: String): List<TimedLyric> {
    val paragraph = Regex("<p\\b([^>]*)>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE)
    val result = paragraph.findAll(source).mapNotNull { p ->
        val attrs = p.groupValues[1]; val body = p.groupValues[2]
        val begin = Regex("begin=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1)?.let(::ttmlTime) ?: 0
        val role = Regex("role=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1).orEmpty()
        val plain = body.replace(Regex("<[^>]+>"), "").trim()
        if (plain.isBlank()) return@mapNotNull null
        val words = if (role.isBlank()) Regex("<span\\b([^>]*)>([\\s\\S]*?)</span>", RegexOption.IGNORE_CASE).findAll(body).mapNotNull { s ->
            val t = Regex("begin=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE).find(s.groupValues[1])?.groupValues?.get(1) ?: return@mapNotNull null
            TimedWord(begin + ttmlTime(t), s.groupValues[2].replace(Regex("<[^>]+>"), ""))
        }.toList() else emptyList()
        TimedLyric(begin, plain, words, role.isBlank(), if (role.contains("roman", true) || role.contains("roma", true)) plain else null, if (role.contains("translation", true)) plain else null)
    }.toList().sortedBy { it.timeMs }
    return result.ifEmpty { listOf(TimedLyric(0, "暂无内嵌歌词")) }
}

private fun ttmlTime(value: String): Long = when {
    value.endsWith("ms", true) -> value.dropLast(2).toLongOrNull() ?: 0
    value.endsWith("s", true) -> ((value.dropLast(1).toDoubleOrNull() ?: 0.0) * 1000).toLong()
    else -> 0
}

private fun String.isMostlyAscii(): Boolean {
    val letters = count { !it.isWhitespace() }
    return letters > 0 && count { it.code < 128 } * 10 >= letters * 7
}

private fun Char.isLatinOrDigit() = isLetterOrDigit() && code < 128

private fun parseLyricTime(match: MatchResult): Long {
    val fraction = match.groupValues[4].padEnd(3, '0').take(3).toLong()
    return (match.groupValues[2].toLong() * 60 + match.groupValues[3].toLong()) * 1000 + fraction
}

private fun parseLyricTime(line: String): Long = Regex("\\[(\\d+):(\\d{2})[.:](\\d{1,3})]").find(line)?.let {
    val f = it.groupValues[3].padEnd(3, '0').take(3).toLong(); (it.groupValues[1].toLong() * 60 + it.groupValues[2].toLong()) * 1000 + f
} ?: 0

@Composable
private fun LyricsPage(song: Song, position: Long, colors: CoverColors) {
    val lines = timedLyrics(song)
    val currentIndex = lines.indexOfLast { it.primary && it.timeMs <= position }
    val listState = rememberLazyListState()
    LaunchedEffect(song.id, currentIndex) { if (currentIndex >= 0) listState.animateScrollToItem(currentIndex) }
    Column(Modifier.fillMaxSize().padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 180.dp)) {
            itemsIndexed(lines) { index, line ->
                if (line.primary) {
                    line.roma?.let { Text(it, color = colors.muted.copy(alpha = .5f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 2.dp)) }
                }
                if (line.words.isEmpty()) Text(
                    line.text,
                    color = if (index == currentIndex) colors.accent else colors.muted.copy(alpha = .45f),
                    fontSize = if (index == currentIndex) 29.sp else 23.sp,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                ) else Row(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    line.words.forEachIndexed { wordIndex, word ->
                        val nextTime = line.words.getOrNull(wordIndex + 1)?.timeMs ?: Long.MAX_VALUE
                        val active = line.primary && position >= word.timeMs && position < nextTime
                        val sung = position >= nextTime
                        // 已播放字词保持抬升和高亮，当前字词使用更强的强调动画
                        val scale by animateFloatAsState(if (active) 1.16f else if (sung) 1.06f else 1f, tween(220), label = "word-scale")
                        val lift by animateFloatAsState(if (active) -8f else if (sung) -4f else 0f, tween(220), label = "word-lift")
                        Text(
                            word.text,
                            color = when { active -> colors.accent; sung -> colors.accent.copy(alpha = .82f); else -> colors.muted.copy(alpha = .5f) },
                            fontSize = if (index == currentIndex) 25.sp else 21.sp,
                            fontWeight = if (active || index == currentIndex) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationY = lift }
                        )
                    }
                }
                if (line.primary) {
                    line.translation?.let { Text(it, color = colors.muted.copy(alpha = .6f), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("词", color = colors.accent, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(colors.surface.copy(alpha = .7f)).padding(horizontal = 7.dp, vertical = 4.dp))
            Spacer(Modifier.width(10.dp))
            Text("EMBEDDED", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DetailPage(song: Song, colors: CoverColors) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 25.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard("歌曲", listOf(song.title, song.artist, song.album), colors)
        val format = song.format?.let { "$it format stream" } ?: "Audio format stream"
        val technical = buildString {
            song.channels?.let { append("$it Channels") }
            song.sampleRateHz?.let { if (isNotEmpty()) append("    "); append("$it Hz") }
            song.bitrateKbps?.let { if (isNotEmpty()) append("    "); append("$it kbps") }
        }.ifBlank { "Metadata unavailable" }
        InfoCard("音频信息", listOf(format, technical), colors)
        InfoCard("标签", listOf(song.composer?.let { "作曲：$it" } ?: "作曲信息未知", song.genre?.let { "流派：$it" } ?: "流派信息未知", "第 ${song.trackNumber} 首"), colors)
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
private fun InfoCard(title: String, values: List<String>, colors: CoverColors) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(24.dp)) {
        Text(title, color = colors.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        values.forEach { Text(it, color = colors.muted, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp)) }
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
