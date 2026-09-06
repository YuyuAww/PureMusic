package com.pure.music.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.Glance
import androidx.glance.GlanceId
import androidx.glance.action.GlanceAction
import androidx.glance.action.rememberGlanceAction
import androidx.glance.appwidget.GlanceAppCompatWidget
import androidx.glance.appwidget.GlanceState
import androidx.glance.appwidget.actionReceiverClass
import androidx.glance.appwidget.context.ContextState
import androidx.glance.appwidget.rememberGlanceContext
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.MaterialTheme
import androidx.glance.material3.Text
import androidx.glance.material3.TextButton
import androidx.glance.text.TextOverflow
import com.pure.music.player.PlaybackState
import com.pure.music.player.PlayerManager

/**
 * 桌面迷你播放器小部件，基于 Glance 框架。
 * 显示当前播放歌曲信息和播放/暂停、下一首按钮。
 */
class NowPlayingWidget : GlanceAppCompatWidget() {
    override val state: GlanceState = androidx.glance.appwidget.AppWidgetState(
        context = ContextState(context = ::rememberGlanceContext),
        actionReceiverClass = NowPlayingWidgetReceiver::class.java
    )

    companion object {
        val Glance: Glance = NowPlayingWidgetGlance
    }
}

/** Glance 内容定义，在 provideGlance 中组合小部件 UI */
val NowPlayingWidgetGlance = object : Glance() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val playbackState = PlayerManager.state.value

        if (playbackState.currentSong != null) {
            NowPlayingContent(playbackState, context)
        } else {
            Text(
                text = "PureMusic",
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

/** 正在播放内容：封面占位、歌曲信息、控制按钮 */
@androidx.compose.runtime.Composable
private fun NowPlayingContent(state: PlaybackState, context: Context) {
    val song = state.currentSong ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面占位符
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "♫", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.width(12.dp))

        // 歌曲标题和艺术家
        Column {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // 播放/暂停按钮
        TextButton(
            onClick = rememberGlanceAction(
                GlanceAction.Run(
                    intent = Intent(context, NowPlayingWidgetReceiver::class.java).apply {
                        action = "toggle_play_pause"
                    },
                    id = "toggle_play_pause"
                )
            ),
            text = {
                Text(if (state.isPlaying) "⏸" else "▶")
            }
        )

        Spacer(Modifier.width(4.dp))

        // 下一首按钮
        TextButton(
            onClick = rememberGlanceAction(
                GlanceAction.Run(
                    intent = Intent(context, NowPlayingWidgetReceiver::class.java).apply {
                        action = "next"
                    },
                    id = "next"
                )
            ),
            text = {
                Text("⏭")
            }
        )
    }
}
