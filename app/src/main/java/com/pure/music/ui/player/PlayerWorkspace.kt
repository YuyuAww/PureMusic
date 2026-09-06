package com.pure.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import com.pure.music.player.PlaybackState
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.library.formatDuration

/** 统一播放工作区：顶部、底部控制在详情/歌词/播放内容之间共享。 */
@Composable
fun PlayerWorkspace(
    state: PlaybackState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onShuffleMode: (Boolean) -> Unit
) {
    val song = state.currentSong ?: return
    var page by remember { mutableStateOf(WorkspacePage.PLAYER) }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { page = WorkspacePage.DETAIL }) { Icon(Icons.Default.Info, "歌曲详情") }
                IconButton(onClick = { page = WorkspacePage.PLAYER }) { Icon(Icons.Default.Album, "播放页") }
                IconButton(onClick = { page = WorkspacePage.LYRICS }) { Icon(Icons.Default.Notes, "歌词页") }
                IconButton(onClick = {}) { Icon(Icons.Default.Cast, contentDescription = "投屏") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp)) {
                Slider(
                    value = state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                    valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                    onValueChange = { onSeek(it.toLong()) }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(state.position), fontSize = 12.sp)
                    Text(formatDuration(state.duration), fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "上一首") }
                    FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(64.dp), shape = CircleShape) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "暂停" else "播放", Modifier.size(34.dp))
                    }
                    IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "下一首") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShuffleMode(!state.shuffleModeEnabled) }) {
                        Icon(Icons.Default.Shuffle, "随机播放", tint = if (state.shuffleModeEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.Timer, "睡眠定时") }
                    IconButton(onClick = {}) { Icon(Icons.Default.GraphicEq, "音频可视化") }
                    IconButton(onClick = {}) { Icon(Icons.Default.QueueMusic, "播放队列") }
                    IconButton(onClick = {
                        val next = when (state.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                        }
                        onRepeatMode(next)
                    }) { Icon(Icons.Default.Repeat, "循环播放") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, "更多") }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
            when (page) {
                WorkspacePage.DETAIL -> InfoPanel("歌曲详情", Modifier.fillMaxSize()) {
                Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(song.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Text("本地音乐", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatDuration(song.duration)} · ${song.uri.lastPathSegment ?: "音频文件"}", style = MaterialTheme.typography.bodySmall)
                }
                WorkspacePage.PLAYER -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                AlbumArt(song, Modifier.sizeIn(maxWidth = 360.dp, maxHeight = 360.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
                Spacer(Modifier.height(12.dp))
                // 迷你歌词窗：后续由 LRC 解析器根据 position 同步当前行
                Surface(
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("歌词", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("暂无同步歌词", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                AudioVisualizer(isPlaying = state.isPlaying, modifier = Modifier.fillMaxWidth().height(64.dp))
                }
                WorkspacePage.LYRICS -> InfoPanel("歌词", Modifier.fillMaxSize()) {
                Text("暂无歌词", modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private enum class WorkspacePage { DETAIL, PLAYER, LYRICS }

@Composable
private fun AudioVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        repeat(24) { index ->
            val height = if (isPlaying) (18 + (index * 17 % 38)).dp else 12.dp
            Box(Modifier.width(3.dp).height(height).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)))
        }
    }
}

@Composable
private fun InfoPanel(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.Top, content = { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); content() })
    }
}
