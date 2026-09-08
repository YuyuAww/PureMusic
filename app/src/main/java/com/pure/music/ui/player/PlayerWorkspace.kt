package com.pure.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import com.pure.music.data.Song
import com.pure.music.player.PlaybackState
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration

/** Salt Player 风格的单一播放工作区：顶部歌曲信息，内容页切换，底部固定控制栏。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerWorkspace(state: PlaybackState, onDismiss: () -> Unit, onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Long) -> Unit, onRepeatMode: (Int) -> Unit, onShuffleMode: (Boolean) -> Unit) {
    val song = state.currentSong ?: return
    var page by remember { mutableStateOf(PlayerPage.COVER) }
    Scaffold(topBar = { TopAppBar(title = { Column { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.KeyboardArrowDown, "关闭播放页") } }, actions = { IconButton(onClick = { page = PlayerPage.QUEUE }) { Icon(Icons.Default.QueueMusic, "播放队列") }; IconButton(onClick = { page = PlayerPage.LYRICS }) { Icon(Icons.Default.Notes, "歌词") } }) }, bottomBar = { PlayerControls(state, onTogglePlayPause, onNext, onPrevious, onSeek, onRepeatMode, onShuffleMode) }) { padding -> Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) { when (page) { PlayerPage.COVER -> CoverPage(song); PlayerPage.LYRICS -> EmptyPage("暂无同步歌词", Icons.Default.Notes); PlayerPage.DETAIL -> DetailPage(song); PlayerPage.QUEUE -> QueuePage(state) } } }
}

private enum class PlayerPage { COVER, LYRICS, DETAIL, QUEUE }
@Composable private fun CoverPage(song: Song) { Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF0DE), Color(0xFFF8F4FF)))).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(24.dp)); Text(song.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Text(song.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(28.dp)); AlbumArt(song, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))); Spacer(Modifier.height(26.dp)); Text("暂无同步歌词", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary); Text("歌词将在播放时显示", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun DetailPage(song: Song) { Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("歌曲详情", style = MaterialTheme.typography.headlineSmall); DetailRow("标题", song.title); DetailRow("艺术家", song.artist); DetailRow("专辑", song.album); DetailRow("时长", formatDuration(song.duration)); DetailRow("文件", song.uri.lastPathSegment ?: "本地音频") } }
@Composable private fun DetailRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, modifier = Modifier.widthIn(max = 240.dp), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun EmptyPage(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun QueuePage(state: PlaybackState) { LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) { itemsIndexed(state.queue, key = { _, it -> it.id }) { index, song -> Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (index == state.queueIndex) Icons.Default.GraphicEq else Icons.Default.MusicNote, null, tint = if (index == state.queueIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(formatDuration(song.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PlayerControls(state: PlaybackState, onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Long) -> Unit, onRepeatMode: (Int) -> Unit, onShuffleMode: (Boolean) -> Unit) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { Slider(value = state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()), valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), onValueChange = { onSeek(it.toLong()) }); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatDuration(state.position), fontSize = 12.sp); Text(formatDuration(state.duration), fontSize = 12.sp) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "上一首") }; FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(64.dp), shape = CircleShape) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(34.dp)) }; IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "下一首") } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { IconButton(onClick = { onShuffleMode(!state.shuffleModeEnabled) }) { Icon(Icons.Default.Shuffle, "随机") }; IconButton(onClick = { onRepeatMode(if (state.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else if (state.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF) }) { Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "循环") }; IconButton(onClick = {}) { Icon(Icons.Default.Timer, "定时") } } } }
