package com.pure.music.player

import com.pure.music.player.dsp.CustomEqualizerAudioProcessor
import com.pure.music.player.dsp.FilterParam
import com.pure.music.player.dsp.FilterType

/** 应用内 DSP 均衡器状态与 UI 的桥接。 */
object EqualizerController {
    const val BAND_COUNT = 10
    private val frequencies = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    private const val MAX_GAIN_DB = 12.0
    private val processor = CustomEqualizerAudioProcessor()
    private var enabled = true
    private var levels = List(BAND_COUNT) { 0f }

    val isAvailable: Boolean get() = true
    val isEnabled: Boolean get() = enabled
    val bandLevels: List<Float> get() = levels
    val audioProcessor: CustomEqualizerAudioProcessor get() = processor

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        processor.setEnabled(enabled)
    }

    /** value 为 -1f..1f，映射到每段 -12..12 dB。 */
    fun setBandLevel(index: Int, value: Float) {
        if (index !in levels.indices) return
        levels = levels.toMutableList().also { it[index] = value.coerceIn(-1f, 1f) }
        processor.setParams(frequencies.mapIndexed { i, frequency ->
            FilterParam(FilterType.PEAK, frequency, levels[i] * MAX_GAIN_DB, q = 1.0)
        })
    }

    fun setPreampGainDb(gainDb: Double) = processor.setPreampGainDb(gainDb)

    fun release() {
        processor.onReset()
    }
}
