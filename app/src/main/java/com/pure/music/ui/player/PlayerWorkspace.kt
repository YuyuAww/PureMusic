package com.pure.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pure.music.data.Song
import com.pure.music.player.PlaybackState
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors

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
    onToggleFavorite: () -> Unit = {}
) {
    val song = state.currentSong ?: return
    var lyricsJumpNonce by remember { mutableIntStateOf(0) }
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
    LaunchedEffect(song.albumId) { 
        colors = loadCoverColors(context, song.albumId, fallback) 
    }

    // 使用 Scaffold 划分三大组件区域
    Scaffold(
        containerColor = colors.background, // 整体背景：由封面背景色决定
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 移除默认 Insets，交给子组件自行处理
        topBar = { 
            // 1. TopBar 组件
            PlayerTopBar(song = song, colors = colors) 
        },
        bottomBar = { 
            // 3. BottomBar 组件
            PlayerBottomBar(
                state = state,
                colors = colors,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onRepeatMode = onRepeatMode,
                onShuffleMode = onShuffleMode
            ) 
        }
    ) { paddingValues ->
        // 2. Content (中间内容) 组件
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 22.dp)
        ) { page ->
            when (page) {
                0 -> DetailPage(song, colors)
                1 -> CoverAndLyricsPage(song, colors) { lyricsJumpNonce++ }
                else -> LyricsPage(song, colors)
            }
        }
    }
}

// ---------------------------------------------------------
// 1. TopBar 组件 (包含歌名、歌手、投屏)
// ---------------------------------------------------------
@Composable
private fun PlayerTopBar(song: Song, colors: CoverColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.background) // 背景延伸到状态栏区域
            .statusBarsPadding() // 内容避开状态栏
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 15.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = colors.accent, // 封面主色
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                song.artist,
                color = colors.accent.copy(alpha = 0.85f), // 封面主色（淡化）
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = {}, modifier = Modifier.padding(top = 4.dp)) {
            Icon(
                Icons.Default.Cast,
                "切换播放设备",
                tint = colors.accent, // 封面主色
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------------------------------------------------
// 2. 中间内容组件：歌曲封面 + 迷你歌词窗
// ---------------------------------------------------------
@Composable
private fun CoverAndLyricsPage(song: Song, colors: CoverColors, onOpenLyrics: () -> Unit) {
    val lines = placeholderLyrics(song)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 歌曲封面
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface) // 封面卡片背景：由封面表面色决定
        ) {
            AlbumArt(song, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(35.dp))
        // 迷你歌词窗
        MiniLyricsWindow(lines[1], lines[2], lines[3], colors, onOpenLyrics)
    }
}

@Composable
private fun MiniLyricsWindow(current: String, next1: String, next2: String, colors: CoverColors, onCurrentClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(current, color = colors.accent.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onCurrentClick() }.padding(vertical = 9.dp))
        Text(next1, color = colors.accent.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 9.dp))
        Text(next2, color = colors.accent.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 9.dp))
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
    onShuffleMode: (Boolean) -> Unit
) {
    var sliderPosition by remember(state.currentSong?.id) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.position, dragging) { if (!dragging) sliderPosition = state.position.toFloat() }
    val displayedPosition = if (dragging) sliderPosition.toLong() else state.position

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background) // 底部背景：与 TopBar 保持一致
            .navigationBarsPadding() // 内容避开底部导航栏
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 20.dp)
    ) {
        // 极细进度条
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.accent.copy(alpha = .22f)) // 进度条轨道：封面主色极淡版
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = sliderPosition / state.duration.coerceAtLeast(1).toFloat())
                    .fillMaxHeight()
                    .background(colors.accent) // 已播放进度：封面主色
            )
            // 进度圆点
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (sliderPosition / state.duration.coerceAtLeast(1).toFloat() * 100).dp - 5.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.accent) // 圆点：封面主色
            )
        }

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
            IconButton(onClick = { onShuffleMode(!state.shuffleModeEnabled) }) { Icon(Icons.Default.Repeat, "播放模式", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.Alarm, "定时", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.GraphicEq, "音效", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.QueueMusic, "播放列表", tint = colors.accent, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, "更多", tint = colors.accent, modifier = Modifier.size(24.dp)) }
        }
    }
}

// ---------------------------------------------------------
// 辅助页面 (详情页 / 全屏歌词页 / 占位歌词)
// ---------------------------------------------------------
private fun placeholderLyrics(song: Song) = listOf("听见山林深处的风", "唱一曲少年的梦", song.title, "英雄不怕虎豹", "我娘说四宝你瞧瞧", "田野间群山相望")

@Composable
private fun LyricsPage(song: Song, colors: CoverColors) {
    val lines = placeholderLyrics(song)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            lines.forEachIndexed { index, line ->
                Text(
                    line, 
                    color = if (index == 2) colors.accent else colors.muted.copy(alpha = .32f), // 当前歌词亮色，其他歌词暗色
                    fontSize = if (index == 2) 29.sp else 23.sp, 
                    fontWeight = if (index == 2) FontWeight.Bold else FontWeight.Medium, 
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.padding(vertical = 14.dp)
                )
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
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f)) { DetailCard("☀", "播放界面保持屏幕", colors) }
            Box(Modifier.weight(1f)) { DetailCard("☊", "沉浸模式", colors) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f)) { DetailCard("▦", "Original Sound", colors) }
            Box(Modifier.weight(1f)) { DetailCard("◉", "DLNA (beta)", colors) }
        }
        InfoCard("音频信息", listOf("FLAC format stream", "2 Channels    44100 Hz    828 kbps"), colors)
        InfoCard("出自专辑", listOf(song.album.ifBlank { "原创歌曲合集" }, "未知专辑艺术家"), colors)
        InfoCard("参与创作的艺术家", listOf(song.artist, "9 首"), colors)
    }
}

@Composable
private fun DetailCard(icon: String, text: String, colors: CoverColors) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = colors.accent, fontSize = 28.sp)
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