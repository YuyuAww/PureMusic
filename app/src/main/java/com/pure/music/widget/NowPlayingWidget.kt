package com.pure.music.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.unit.dp
import com.pure.music.player.PlayerManager
import com.pure.music.MainActivity

/** 桌面迷你播放器：点击主体进入应用，按钮直接控制后台播放器。 */
class NowPlayingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = PlayerManager.state.value
            val song = state.currentSong
            if (song == null) {
                Text("PureMusic", modifier = GlanceModifier.padding(16.dp))
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(8.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("♫", modifier = GlanceModifier.size(48.dp).padding(12.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(song.title, maxLines = 1)
                        Text(song.artist, maxLines = 1)
                    }
                    Text(
                        if (state.isPlaying) "Ⅱ" else "▶",
                        modifier = GlanceModifier.padding(8.dp)
                            .clickable(actionRunCallback<TogglePlayPauseAction>())
                    )
                    Text(
                        "▶|",
                        modifier = GlanceModifier.padding(8.dp)
                            .clickable(actionRunCallback<NextAction>())
                    )
                }
            }
        }
    }
}

class TogglePlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        PlayerManager.togglePlayPause()
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        PlayerManager.next()
    }
}
