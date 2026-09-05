package com.pure.music.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppCompatWidget
import androidx.glance.appwidget.GlanceState
import androidx.glance.appwidget.components.ActionReceiverClass
import androidx.glance.appwidget.components.AppWidgetState
import androidx.glance.appwidget.context.ContextState
import androidx.glance.appwidget.context.RememberContextState
import androidx.glance.action.GlanceAction
import androidx.glance.action.GlanceIntent
import androidx.glance.action.action
import androidx.glance.action.rememberGlanceAction
import androidx.glance.appwidget.Glance
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.Icon
import androidx.glance.material3.MaterialTheme
import androidx.glance.material3.Text
import androidx.glance.material3.TextButton
import com.pure.music.data.Song
import com.pure.music.player.PlayerManager
import com.pure.music.ui.library.formatDuration
import kotlinx.coroutines.flow.first

/**
 * 桌面迷你播放器小部件，基于 Glance 框架。
 * 显示当前播放歌曲信息和播放/暂停、下一首按钮。
 */
class NowPlayingWidget : GlanceAppCompatWidget() {

    override val definition: Glance = object : Glance() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            val state = PlayerManager.state.value
            val contextState = rememberGlanceContext()

            Box(
                modifier = androidx.glance.layout.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp)
            ) {
                if (state.currentSong != null) {
                    NowPlayingContent(state, contextState)
                } else {
                    Text(
                        text = "PureMusic",
                        modifier = androidx.glance.layout.fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }

        /** 正在播放内容：封面占位、歌曲信息、控制按钮 */
        @androidx.compose.runtime.Composable
        private fun NowPlayingContent(
            state: com.pure.music.player.PlaybackState,
            context: Context
        ) {
            val song = state.currentSong ?: return

            Row(
                modifier = androidx.glance.layout.fillMaxWidth(),
                alignment = Alignment.CenterVertically
            ) {
                // 封面占位符
                Box(
                    modifier = androidx.glance.layout.size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "♫", style = MaterialTheme.typography.titleMedium)
                }

                androidx.glance.layout.width(12.dp)

                // 歌曲标题和艺术家
                androidx.glance.layout.Column {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.glance.text.TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.glance.text.TextOverflow.Ellipsis
                    )
                }

                androidx.glance.layout.width(8.dp)

                // 播放/暂停按钮
                TextButton(
                    onClick = rememberGlanceAction {
                        GlanceIntent(
                            intent = Intent(
                                NowPlayingWidgetReceiver::class.java,
                                context
                            ).setAction("toggle_play_pause")
                        )
                    },
                    text = {
                        Text(if (state.isPlaying) "⏸" else "▶")
                    }
                )

                androidx.glance.layout.width(4.dp)

                // 下一首按钮
                TextButton(
                    onClick = rememberGlanceAction {
                        GlanceIntent(
                            intent = Intent(
                                NowPlayingWidgetReceiver::class.java,
                                context
                            ).setAction("next")
                        )
                    },
                    text = {
                        Text("⏭")
                    }
                )
            }
        }
    }

    companion object {
        val State: GlanceState = AppWidgetState(
            context = ContextState(context = ::rememberGlanceContext),
            actionReceiverClass = NowPlayingWidgetReceiver::class.java
        )
    }
}
