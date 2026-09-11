package com.pure.music.ui.player

import android.app.Activity
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.pure.music.data.Song
import com.pure.music.player.PlaybackState
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerWorkspace(state: PlaybackState, onDismiss: () -> Unit, onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Long) -> Unit, onRepeatMode: (Int) -> Unit, onShuffleMode: (Boolean) -> Unit, isFavorite: Boolean = false, onToggleFavorite: () -> Unit = {}) {
    val song = state.currentSong ?: return
    var lyricsJumpNonce by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    LaunchedEffect(lyricsJumpNonce) {
        if (lyricsJumpNonce > 0) pagerState.animateScrollToPage(2)
    }
    val fallback = CoverColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
    val context = LocalContext.current
    val view = LocalView.current
    var colors by remember(song.albumId) { mutableStateOf(fallback) }
    LaunchedEffect(song.albumId) { colors = loadCoverColors(context, song.albumId, fallback) }
    // 状态栏图标明暗跟随背景色（封面取色）亮度
    LaunchedEffect(colors) {
        WindowCompat.getInsetsController((view.context as Activity).window, view)
            .isAppearanceLightStatusBars = ColorUtils.calculateLuminance(colors.background.toArgb()) < 0.5f
    }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(containerColor = Color.Transparent, topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text(song.title, color = colors.accent, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(song.artist, color = colors.muted, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                IconButton(onClick = {}) { Icon(Icons.Default.Cast, "切换播放设备", tint = colors.muted, modifier = Modifier.size(30.dp)) }
            }
        }, bottomBar = { PlayerControlsLayered(state, colors, onTogglePlayPause, onNext, onPrevious, onSeek, onRepeatMode, onShuffleMode) }) { padding ->
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) { page -> when (page) {
                0 -> DetailPage(song, colors)
                1 -> CoverPage(song, colors) { lyricsJumpNonce++ }
                else -> LyricsPage(song, colors)
            } }
        }
    }
}

@Composable private fun CoverPage(song: Song, colors: CoverColors, onOpenLyrics: () -> Unit) {
    val lines = placeholderLyrics(song)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shadow(8.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
            AlbumArt(song, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(24.dp))
        MiniLyricsWindow(lines[1], lines[2], lines[3], colors, onOpenLyrics)
    }
}
@Composable private fun MiniLyricsWindow(current: String, next1: String, next2: String, colors: CoverColors, onCurrentClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp), horizontalAlignment = Alignment.Start) {
        Text(current, color = colors.accent, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onCurrentClick() })
        Spacer(Modifier.height(8.dp))
        Text(next1, color = colors.muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(next2, color = colors.muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
private fun placeholderLyrics(song: Song) = listOf("听见山林深处的风", "唱一曲少年的梦", song.title, "英雄不怕虎豹", "我娘说四宝你瞧瞧", "田野间群山相望")

@Composable private fun LyricsPage(song: Song, colors: CoverColors) { val lines = placeholderLyrics(song); Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) { Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { lines.forEachIndexed { index, line -> Text(line, color = if (index == 2) colors.accent else colors.muted.copy(alpha = .32f), fontSize = if (index == 2) 29.sp else 23.sp, fontWeight = if (index == 2) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 14.dp)) } }; Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("词", color = colors.accent, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(colors.surface.copy(alpha = .7f)).padding(horizontal = 7.dp, vertical = 4.dp)); Spacer(Modifier.width(10.dp)); Text("EMBEDDED", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 16.sp) } } }

@Composable private fun DetailPage(song: Song, colors: CoverColors) { Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Box(Modifier.weight(1f)) { DetailCard("☀", "播放界面保持屏幕", colors) }; Box(Modifier.weight(1f)) { DetailCard("☊", "沉浸模式", colors) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Box(Modifier.weight(1f)) { DetailCard("▦", "Original Sound", colors) }; Box(Modifier.weight(1f)) { DetailCard("◉", "DLNA (beta)", colors) } }; InfoCard("音频信息", listOf("FLAC format stream", "2 Channels    44100 Hz    828 kbps"), colors); InfoCard("出自专辑", listOf(song.album.ifBlank { "原创歌曲合集" }, "未知专辑艺术家"), colors); InfoCard("参与创作的艺术家", listOf(song.artist, "9 首"), colors) } }
@Composable private fun DetailCard(icon: String, text: String, colors: CoverColors) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(22.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = colors.accent, fontSize = 28.sp); Spacer(Modifier.width(20.dp)); Text(text, color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun InfoCard(title: String, values: List<String>, colors: CoverColors) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface.copy(alpha = .7f)).padding(24.dp)) { Text(title, color = colors.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold); values.forEach { Text(it, color = colors.muted, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp)) } } }

@Composable private fun PlayerControlsLayered(state: PlaybackState, colors: CoverColors, onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Long) -> Unit, onRepeatMode: (Int) -> Unit, onShuffleMode: (Boolean) -> Unit) {
    var sliderPosition by remember(state.currentSong?.id) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.position, dragging) { if (!dragging) sliderPosition = state.position.toFloat() }
    val displayedPosition = if (dragging) sliderPosition.toLong() else state.position
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = .78f))
            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
    ) {
        // 细线进度条 + 时间
        Slider(
            value = sliderPosition.coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
            onValueChange = { sliderPosition = it; dragging = true },
            onValueChangeFinished = { dragging = false; onSeek(sliderPosition.toLong()) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.muted.copy(alpha = .35f))
        )
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(displayedPosition), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(formatDuration(state.duration), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        // 主控制：上一首 / 播放暂停 / 下一首，无填充背景
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.padding(end = 40.dp)) { Icon(Icons.Default.SkipPrevious, "上一首", tint = colors.accent, modifier = Modifier.size(22.dp)) }
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(46.dp)) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "播放", tint = colors.accent, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onNext, modifier = Modifier.padding(start = 40.dp)) { Icon(Icons.Default.SkipNext, "下一首", tint = colors.accent, modifier = Modifier.size(22.dp)) }
        }
        Spacer(Modifier.height(30.dp))
        // 底部五项功能键
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShuffleMode(!state.shuffleModeEnabled) }) { Icon(Icons.Default.Repeat, "播放模式", tint = colors.accent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.Alarm, "定时", tint = colors.accent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.GraphicEq, "音效", tint = colors.accent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.QueueMusic, "播放列表", tint = colors.accent, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, "更多", tint = colors.accent, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(16.dp))
    }
}
