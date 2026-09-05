package com.pure.music.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pure.music.player.PlayerManager

/** 桌面小部件动作接收器，处理播放/暂停、下一首、上一首按钮点击 */
class NowPlayingWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "toggle_play_pause" -> PlayerManager.togglePlayPause()
            "next" -> PlayerManager.next()
            "previous" -> PlayerManager.previous()
        }
    }
}
