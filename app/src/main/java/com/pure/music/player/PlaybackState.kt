package com.pure.music.player

import androidx.media3.common.Player
import com.pure.music.data.Song

/** 播放器状态，UI 通过观察此对象驱动界面更新 */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1
)
