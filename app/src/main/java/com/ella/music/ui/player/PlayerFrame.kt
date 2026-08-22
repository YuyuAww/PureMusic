package com.ella.music.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.data.model.AudioInfo
import com.ella.music.data.model.Song
import com.ella.music.ui.components.PlayerQueueListIcon

/**
 * Shared top bar for the player: left = song title + artist, right = audio output icon + fav + more.
 */
@Composable
internal fun PlayerTopBar(
    song: Song?,
    annotation: String,
    bluetoothDeviceName: String?,
    isFavorite: Boolean,
    contentColor: Color,
    fontFamily: FontFamily?,
    onArtist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerSongMetaText(
            song = song,
            annotation = annotation,
            titleFontSize = 22.sp,
            artistFontSize = 14.sp,
            artistAlpha = 0.62f,
            showArtistWithAnnotation = true,
            contentColor = contentColor,
            fontFamily = fontFamily,
            onArtistClick = onArtist,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 230.dp)
        )
        if (bluetoothDeviceName != null) {
            Spacer(modifier = Modifier.width(8.dp))
            AudioOutputIcon(
                color = contentColor.copy(alpha = 0.72f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        PlayerHeaderAction(
            kind = PlayerHeaderActionKind.Favorite,
            selected = isFavorite,
            onClick = onToggleFavorite
        )
        PlayerHeaderAction(kind = PlayerHeaderActionKind.More, onClick = onShowMenu)
    }
}

/**
 * Audio output device indicator icon (Bluetooth / wireless wave).
 */
@Composable
internal fun AudioOutputIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.11f
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Outer arc
        val outerR = size.minDimension * 0.38f
        drawArc(
            color = color,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(cx - outerR, cy - outerR),
            size = androidx.compose.ui.geometry.Size(outerR * 2, outerR * 2)
        )
        // Inner arc
        val innerR = size.minDimension * 0.22f
        drawArc(
            color = color,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.8f, cap = StrokeCap.Round),
            topLeft = Offset(cx - innerR, cy - innerR),
            size = androidx.compose.ui.geometry.Size(innerR * 2, innerR * 2)
        )
        // Dot center
        drawCircle(
            color = color,
            radius = stroke * 0.9f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Shared bottom action row: 循环模式 | 睡眠定时 | 音效(均衡器) | 播放列表 | 更多
 */
@Composable
internal fun PlayerBottomActionRow(
    shuffleEnabled: Boolean,
    repeatMode: Int,
    palette: PlayerPalette,
    onCyclePlaybackMode: () -> Unit,
    onTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onToggleQueue: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerTransportIconButton(onClick = onCyclePlaybackMode) {
            PlaybackModeIcon(
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                color = palette.onBackground.copy(alpha = 0.92f)
            )
        }
        PlayerTransportIconButton(onClick = onTimer) {
            QuickActionIcon(
                kind = PlayerQuickActionKind.Timer,
                color = palette.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(22.dp)
            )
        }
        PlayerTransportIconButton(onClick = onOpenEqualizer) {
            QuickActionIcon(
                kind = PlayerQuickActionKind.Equalizer,
                color = palette.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(22.dp)
            )
        }
        PlayerTransportIconButton(onClick = onToggleQueue) {
            PlayerQueueListIcon(
                color = palette.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(22.dp)
            )
        }
        PlayerTransportIconButton(onClick = onMore) {
            QuickActionIcon(
                kind = PlayerQuickActionKind.More,
                color = palette.onBackground.copy(alpha = 0.92f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Shared bottom area: progress bar + transport controls + bottom action row.
 */
@Composable
internal fun PlayerBottomArea(
    currentPosition: Long,
    duration: Long,
    audioInfo: AudioInfo?,
    bluetoothDeviceName: String?,
    musicVideoVisible: Boolean,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    palette: PlayerPalette,
    playerTapSeekEnabled: Boolean,
    playerShowTotalDuration: Boolean,
    queueExpanded: Boolean,
    playlist: List<Song>,
    favoriteSongKeys: Set<String> = emptySet(),
    currentSongKey: String?,
    queueLocked: Boolean,
    loadSongRating: (Song) -> Int = { 0 },
    ratingRevision: Int = 0,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleQueue: () -> Unit,
    onDismissQueue: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onRemoveQueueSong: (Int) -> Unit,
    onMoveQueueSong: (Int, Int) -> Unit,
    onRandomizeQueue: () -> Unit,
    onAddQueueToPlaylist: () -> Unit,
    onClearQueue: () -> Unit,
    onToggleQueueLock: () -> Unit,
    onTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PlayerProgressBlock(
            currentPosition = currentPosition,
            duration = duration,
            audioInfo = audioInfo,
            bluetoothDeviceName = bluetoothDeviceName,
            playbackModeLabel = if (musicVideoVisible) "MV" else null,
            palette = palette,
            allowTapSeek = playerTapSeekEnabled,
            showTotalDuration = playerShowTotalDuration,
            onSeek = onSeek
        )
        Spacer(modifier = Modifier.height(12.dp))
        PlayerTransportControls(
            isPlaying = isPlaying,
            palette = palette,
            queueExpanded = queueExpanded,
            playlist = playlist,
            currentSongKey = currentSongKey,
            queueLocked = queueLocked,
            favoriteSongKeys = favoriteSongKeys,
            loadSongRating = loadSongRating,
            ratingRevision = ratingRevision,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            onCyclePlaybackMode = onCyclePlaybackMode,
            onToggleQueueLock = onToggleQueueLock,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onDismissQueue = onDismissQueue,
            onQueueSongClick = onQueueSongClick,
            onRemoveQueueSong = onRemoveQueueSong,
            onMoveQueueSong = onMoveQueueSong,
            onRandomizeQueue = onRandomizeQueue,
            onAddQueueToPlaylist = onAddQueueToPlaylist,
            onClearQueue = onClearQueue,
            modifier = Modifier.height(76.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        PlayerBottomActionRow(
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            palette = palette,
            onCyclePlaybackMode = onCyclePlaybackMode,
            onTimer = onTimer,
            onOpenEqualizer = onOpenEqualizer,
            onToggleQueue = onToggleQueue,
            onMore = onMore
        )
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}