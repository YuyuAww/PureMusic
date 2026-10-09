package com.pure.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pure.music.player.PlaybackState
import com.pure.music.ui.components.AlbumArt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底部迷你播放器栏（Miuix 卡片风格）：封面、歌曲信息、播放/暂停、收藏与切歌。
 * 点击整栏展开全屏正在播放界面；横向滑动可切歌。
 * 外层 Miuix Scaffold 的 floatingToolbar 槽位负责定位与间距。
 */
@Composable
fun MiniPlayerBar(
    state: PlaybackState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    val colors = MiuixTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .drawWithContent {
                drawContent()
                val inset = 1.5.dp.toPx()
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(inset, inset, size.width - inset, size.height - inset, 20.dp.toPx(), 20.dp.toPx())
                    )
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val progressPath = Path()
                measure.getSegment(
                    0f,
                    measure.length * (state.position.toFloat() / state.duration.coerceAtLeast(1)).coerceIn(0f, 1f),
                    progressPath,
                    true
                )
                drawPath(
                    progressPath,
                    colors.primary,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        shadowElevation = 4.dp,
    ) {
        var swipeDistance by remember(song.id) { mutableFloatStateOf(0f) }
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                swipeDistance > 64.dp.toPx() -> onPrevious()
                                swipeDistance < -64.dp.toPx() -> onNext()
                            }
                            swipeDistance = 0f
                        },
                        onDragCancel = { swipeDistance = 0f },
                        onHorizontalDrag = { change, amount ->
                            swipeDistance += amount
                            change.consume()
                        },
                    )
                },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(song, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (state.isPlaying) MiuixIcons.Pause else MiuixIcons.Play,
                        if (state.isPlaying) "暂停" else "播放",
                        tint = colors.primary,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        if (isFavorite) "取消收藏" else "收藏",
                        tint = colors.primary,
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.SkipPrevious,
                        "上一首",
                        tint = colors.onSurfaceVariantActions,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.SkipNext,
                        "下一首",
                        tint = colors.onSurfaceVariantActions,
                    )
                }
            }
        }
    }
}
