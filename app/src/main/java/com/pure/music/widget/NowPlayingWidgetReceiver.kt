package com.pure.music.widget

import android.content.Context
import androidx.glance.action.GlanceAction
import androidx.glance.action.GlanceActionReceiver
import com.pure.music.player.PlayerManager

/** 桌面小部件动作接收器，处理播放/暂停、下一首按钮点击 */
class NowPlayingWidgetReceiver : GlanceActionReceiver() {
    override suspend fun onAction(context: Context, action: GlanceAction) {
        when (action) {
            is GlanceAction.Run -> {
                when (action.id) {
                    "toggle_play_pause" -> PlayerManager.togglePlayPause()
                    "next" -> PlayerManager.next()
                    "previous" -> PlayerManager.previous()
                }
            }
            else -> super.onAction(context, action)
        }
    }
}
