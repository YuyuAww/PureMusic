package com.pure.music.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pure.music.data.Song
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放器控制层。UI 只操作 MediaController 的代理方法，实际 ExoPlayer
 * 由 PlaybackService 持有；StateFlow 用于向界面同步播放状态。
 */
object PlayerManager {

    private var context: Context? = null
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PlaybackState())
    val state: kotlinx.coroutines.flow.StateFlow<PlaybackState> = _state

    /** 将 Media3 事件转换为应用状态，并维护进度刷新和播放历史。 */
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
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
            if (song != null) recordHistory(song)
            notifyWidgetUpdate()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(
                    duration = controller?.duration ?: 0
                )
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.value = _state.value.copy(repeatMode = repeatMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.value = _state.value.copy(shuffleModeEnabled = shuffleModeEnabled)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Long, newPosition: Long, reason: Int
        ) {
            _state.value = _state.value.copy(position = newPosition)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                errorMessage = error.localizedMessage ?: "无法播放此音频文件"
            )
            stopPositionUpdates()
        }
    }

    /** 按需连接后台播放服务；连接完成前的播放请求会暂存。 */
    fun init(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        _state.value = _state.value.copy(isConnecting = true, errorMessage = null)

        val token = SessionToken(
            this.context!!,
            Intent(this.context, PlaybackService::class.java)
        )
        val future: ListenableFuture<MediaController> =
            MediaController.Builder(this.context!!, token).buildAsync()

        future.addListener({
            try {
                val c = future.get()
                c.addListener(listener)
                controller = c
                _state.value = _state.value.copy(isConnecting = false)
                pendingQueue?.let { (queue, index) ->
                    pendingQueue = null
                    playQueue(queue, index)
                }
            } catch (_: Exception) {
                context = null
                _state.value = _state.value.copy(
                    isConnecting = false,
                    errorMessage = "播放器连接失败，请重试"
                )
            }
        }, Executor { it.run() })
    }

    /** 校验队列后提交给 Media3，并立即更新界面状态。 */
    fun playQueue(queue: List<Song>, startIndex: Int) {
        if (queue.isEmpty() || startIndex !in queue.indices) return
        val c = controller
        if (c == null) {
            pendingQueue = queue to startIndex
            return
        }
        _state.value = _state.value.copy(
            queue = queue,
            queueIndex = startIndex,
            currentSong = queue[startIndex],
            errorMessage = null
        )
        val mediaItems = queue.map { songToMediaItem(it) }
        c.setMediaItems(mediaItems, startIndex, 0)
        c.play()
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOf(song)
        if (index >= 0) playQueue(queue, index)
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun clearError() { _state.value = _state.value.copy(errorMessage = null) }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { c ->
                    _state.value = _state.value.copy(
                        position = c.currentPosition,
                        duration = c.duration.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun recordHistory(song: Song) {
        val ctx = context ?: return
        scope.launch(Dispatchers.IO) {
            com.pure.music.data.db.AppDatabase.get(ctx).historyDao()
                .record(com.pure.music.data.db.PlayHistoryEntity(song.id, System.currentTimeMillis()))
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(position: Long) { controller?.seekTo(position) }
    fun setRepeatMode(mode: Int) { controller?.repeatMode = mode }
    fun setShuffleMode(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }

    /** 释放 Controller、进度协程和待处理播放请求。 */
    fun release() {
        controller?.let {
            it.removeListener(listener)
            it.release()
        }
        controller = null
        context = null
        pendingQueue = null
        stopPositionUpdates()
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
                val widgetManager = androidx.appwidget.AppWidgetManager.getInstance(ctx)
                val component = android.content.ComponentName(
                    ctx, com.pure.music.widget.NowPlayingWidgetReceiver::class.java
                )
                val widgetIds = widgetManager.getAppWidgetIds(component)
                widgetIds.forEach { id ->
                    widgetManager.notifyAppWidgetViewDataChanged(id, 1)
                }
            } catch (_: Exception) {
                // 小部件未安装时忽略
            }
        }
    }
}
