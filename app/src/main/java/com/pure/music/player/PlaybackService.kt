package com.pure.music.player

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pure.music.MainActivity

/**
 * 媒体播放前台服务，基于 Media3 MediaSessionService。
 * 持有 ExoPlayer 实例，提供系统通知栏和锁屏控制。
 */
class PlaybackService : MediaSessionService() {

    override fun onCreateSession(): MediaSession {
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                handleAudioFocus = true
            )
            .build()

        // 点击通知跳转到主界面
        val sessionIntent = Intent(this, MainActivity::class.java)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }
}
