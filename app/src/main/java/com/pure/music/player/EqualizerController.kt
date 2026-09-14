package com.pure.music.player

import android.media.audiofx.Equalizer

/**
 * 把 Compose 控件桥接到 ExoPlayer 的音频会话（系统 AudioFX 均衡器）。
 * 同时在进程内记住开关状态与各频段增益：播放器重建会话重新 attach 时
 * 恢复用户曲线，UI 重新进入时读取到上次设置。
 */
object EqualizerController {
    /** 界面固定 10 个频段，按比例映射到系统均衡器的实际频段数 */
    const val BAND_COUNT = 10

    private var equalizer: Equalizer? = null
    private var enabled = true
    private var levels = List(BAND_COUNT) { 0f }

    /** 系统均衡器是否可用（attach 成功） */
    val isAvailable: Boolean get() = equalizer != null
    val isEnabled: Boolean get() = enabled
    val bandLevels: List<Float> get() = levels

    /** 由播放服务在构建播放器时调用，把系统均衡器挂到当前音频会话 */
    fun attach(audioSessionId: Int) {
        equalizer?.release()
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = enabled
                levels.forEachIndexed { i, v -> setDeviceBand(eq, i, v) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { equalizer?.enabled = enabled }
    }

    /** value 为 -1f..1f，线性映射到设备频段增益的最小/最大 mB 值 */
    fun setBandLevel(index: Int, value: Float) {
        if (index !in levels.indices) return
        levels = levels.toMutableList().also { it[index] = value }
        runCatching { equalizer?.let { setDeviceBand(it, index, value) } }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    private fun setDeviceBand(eq: Equalizer, index: Int, value: Float) {
        val count = eq.numberOfBands.toInt()
        if (count == 0) return
        val band = ((index.toFloat() / (BAND_COUNT - 1)) * (count - 1)).toInt().coerceIn(0, count - 1)
        val range = eq.bandLevelRange
        val level = (range[0] + ((value + 1f) / 2f) * (range[1] - range[0])).toInt()
        eq.setBandLevel(band.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
    }
}
