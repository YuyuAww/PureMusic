package com.pure.music.player

import androidx.media3.common.Player
import com.pure.music.data.Song

/** 播放器对外状态：播放控制、队列、进度、连接状态和可展示的错误信息。 */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val sleepMinutes: Int = 0,
    val errorMessage: String? = null,
    val isConnecting: Boolean = false
)
