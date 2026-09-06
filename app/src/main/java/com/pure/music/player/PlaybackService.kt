package com.pure.music.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pure.music.MainActivity

/**
 * Media3 后台播放服务。ExoPlayer 只在 MediaSession 请求到来时创建，
 * 服务销毁时同时释放播放器和会话，通知栏、锁屏及耳机按键由 Media3 接管。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onGetSession(): MediaSession {
        mediaSession?.let { return it }
        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
        newPlayer.setHandleAudioBecomingNoisy(true)

        // 点击通知跳转到主界面
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        player = newPlayer
        return MediaSession.Builder(this, newPlayer)
            .setSessionActivity(sessionActivity)
            .build()
            .also { mediaSession = it }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
        }
        player?.release()
        player = null
        mediaSession = null
        super.onDestroy()
    }
}
