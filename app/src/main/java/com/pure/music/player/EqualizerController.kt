package com.pure.music.player

import android.media.audiofx.Equalizer

/** Bridges the Compose controls to the ExoPlayer audio session. */
object EqualizerController {
    private var equalizer: Equalizer? = null
    fun attach(audioSessionId: Int) {
        equalizer?.release()
        equalizer = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
    }
    fun setEnabled(enabled: Boolean) { runCatching { equalizer?.enabled = enabled } }
    fun setBandLevel(index: Int, value: Float) {
        runCatching {
            val eq = equalizer ?: return
            val count = eq.numberOfBands.toInt()
            if (count == 0) return
            val band = ((index.toFloat() / 9f) * (count - 1)).toInt().coerceIn(0, count - 1)
            val range = eq.bandLevelRange
            val level = (range[0] + ((value + 1f) / 2f) * (range[1] - range[0])).toInt()
            eq.setBandLevel(band.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
        }
    }
    fun release() { equalizer?.release(); equalizer = null }
}
