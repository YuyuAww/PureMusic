package com.pure.music.player

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.pure.music.data.Song

/**
 * 播放器管理器，持有 MediaController 实例并提供全局播放控制。
 * 通过 StateFlow 暴露播放状态，UI 层观察此状态驱动界面。
 * 同时负责通知桌面小部件更新。
 */
object PlayerManager {

    private var context: Context? = null
    private var controller: MediaController? = null

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PlaybackState())
    val state: kotlinx.coroutines.flow.StateFlow<PlaybackState> = _state

    /** MediaController 事件监听器，将播放器事件同步到 StateFlow */
    private val listener = object : MediaController.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _state.value = _state.value.copy(isPlaying = playing)
            notifyWidgetUpdate()
        }

        override fun onCurrentMediaItemChanged(mediaItem: MediaItem?) {
            val index = controller?.currentMediaItemIndex ?: -1
            val queue = _state.value.queue
            val song = if (index in queue.indices) queue[index] else null
            _state.value = _state.value.copy(
                currentSong = song,
                queueIndex = index,
                duration = controller?.duration ?: 0,
                position = controller?.currentPosition ?: 0
            )
            notifyWidgetUpdate()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _state.value = _state.value.copy(
                        duration = controller?.duration ?: 0
                    )
                }
                Player.STATE_ENDED -> {
                    // 循环模式由 ExoPlayer 内部处理
                }
            }
        }

        override fun onRepeatModeChanged(mode: Int) {
            _state.value = _state.value.copy(repeatMode = mode)
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _state.value = _state.value.copy(shuffleModeEnabled = enabled)
        }

        override fun onTimingsUpdate(position: Long, bufferPercentage: Int, currentBufferedPosition: Long) {
            _state.value = _state.value.copy(position = position)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // 可在后续阶段增加错误 UI 展示
        }
    }

    /** 初始化 MediaController 并绑定播放服务 */
    fun init(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val c = MediaController.Builder(this.context!!, PlaybackService::class.java).buildAsync()
        c.addListener(listener)
        controller = c
    }

    /** 设置播放队列并从指定索引开始播放 */
    fun playQueue(queue: List<Song>, startIndex: Int) {
        val c = controller ?: return
        val mediaItems = queue.map { songToMediaItem(it) }
        c.setMediaItems(mediaItems, startIndex, 0)
        c.play()
        _state.value = _state.value.copy(queue = queue, queueIndex = startIndex)
    }

    fun playSong(song: Song, queue: List<Song>) {
        playQueue(queue, queue.indexOf(song))
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffleMode(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    /** 释放 MediaController 资源 */
    fun release() {
        controller?.let {
            it.removeListener(listener)
            it.release()
        }
        controller = null
        context = null
    }

    /** 将 Song 数据转换为 Media3 MediaItem */
    private fun songToMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .build()

    /** 通知桌面小部件刷新 UI */
    private fun notifyWidgetUpdate() {
        context?.let { ctx ->
            try {
                GlanceManager.getInstance(ctx).update(
                    GlanceId(com.pure.music.widget.NowPlayingWidget::class.java)
                )
            } catch (_: Exception) {
                // 小部件未安装时忽略
            }
        }
    }
}
