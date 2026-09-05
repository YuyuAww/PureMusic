package com.pure.music.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pure.music.data.Song
import com.pure.music.data.db.PlaylistEntity

/**
 * 歌曲上下文菜单对话框，提供收藏和添加到歌单功能。
 * 通过列表展示现有歌单供选择。
 */
@Composable
fun SongMenuDialog(
    song: Song,
    isFavorite: Boolean,
    playlists: List<PlaylistEntity>,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
        },
        text = {
            Column {
                // 收藏/取消收藏
                TextButton(onClick = {
                    onToggleFavorite()
                    onDismiss()
                }) {
                    Text(if (isFavorite) "取消收藏" else "收藏")
                }

                if (playlists.isNotEmpty()) {
                    // 添加到歌单列表
                    Text(
                        text = "添加到歌单",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                onAddToPlaylist(playlist.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playlist.name)
                        }
                    }
                } else {
                    Text(
                        text = "暂无歌单，请先创建",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
