package com.pure.music.player.dsp

import androidx.media3.common.AudioFormat
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.pow

enum class FilterType { PEAK, LOW_SHELF, HIGH_SHELF }

data class FilterParam(
    val type: FilterType,
    val frequencyHz: Double,
    val gainDb: Double = 0.0,
    val q: Double = 1.0,
    val enabled: Boolean = true
)

class CustomEqualizerAudioProcessor : BaseAudioProcessor() {
    @Volatile private var enabled = true
    @Volatile private var preampGain = 1.0
    private var sampleRateHz = 0
    private var channelCount = 0
    private var currentParams: List<FilterParam> = emptyList()
    private val pendingParams = AtomicReference<List<FilterParam>?>(null)
    private val pendingPreamp = AtomicReference<Double?>(null)
    private var filtersByChannel: Array<List<BiquadFilter>> = emptyArray()

    fun setEnabled(enabled: Boolean) { this.enabled = enabled }

    fun setPreampGainDb(gainDb: Double) {
        pendingPreamp.set(10.0.pow(gainDb / 20.0))
    }

    fun setParams(params: List<FilterParam>) { pendingParams.set(params.toList()) }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRateHz = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        currentParams = pendingParams.getAndSet(null) ?: currentParams
        rebuildFilters(currentParams)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        applyPending()
        if (!enabled || filtersByChannel.isEmpty()) {
            val output = replaceOutputBuffer(inputBuffer.remaining())
            output.put(inputBuffer)
            output.flip()
            return
        }

        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frameCount = input.remaining() / channelCount
        val output = replaceOutputBuffer(frameCount * channelCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        val shorts = output.asShortBuffer()
        repeat(frameCount) {
            repeat(channelCount) { channel ->
                var sample = input.get().toDouble() / 32768.0 * preampGain
                filtersByChannel[channel].forEach { sample = it.process(sample) }
                shorts.put((sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.position(shorts.position() * 2)
        output.flip()
    }

    override fun onReset() {
        filtersByChannel.forEach { filters -> filters.forEach(BiquadFilter::reset) }
    }

    override fun isActive(): Boolean = enabled && currentParams.any { it.enabled }

    private fun applyPending() {
        pendingParams.getAndSet(null)?.let {
            currentParams = it
            rebuildFilters(it)
        }
        pendingPreamp.getAndSet(null)?.let { preampGain = it }
    }

    private fun rebuildFilters(params: List<FilterParam>) {
        if (sampleRateHz <= 0 || channelCount <= 0) return
        val active = params.filter { it.enabled && it.frequencyHz > 0.0 && it.frequencyHz < sampleRateHz / 2.0 }
        filtersByChannel = Array(channelCount) {
            active.map { param ->
                BiquadFilter().apply {
                    when (param.type) {
                        FilterType.PEAK -> setPeak(param.frequencyHz, sampleRateHz.toDouble(), param.gainDb, param.q)
                        FilterType.LOW_SHELF -> setLowShelf(param.frequencyHz, sampleRateHz.toDouble(), param.gainDb, param.q)
                        FilterType.HIGH_SHELF -> setHighShelf(param.frequencyHz, sampleRateHz.toDouble(), param.gainDb, param.q)
                    }
                }
            }
        }
    }
}
