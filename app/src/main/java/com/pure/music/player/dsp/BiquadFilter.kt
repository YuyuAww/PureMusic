package com.pure.music.player.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal class BiquadFilter {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }

    fun setPeak(freq: Double, sampleRate: Double, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * freq / sampleRate
        val sn = sin(omega); val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.01))
        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = -2.0 * cs / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = -2.0 * cs / a0
        a2 = (1.0 - alpha / a) / a0
    }

    fun setLowShelf(freq: Double, sampleRate: Double, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * freq / sampleRate
        val sn = sin(omega); val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.01))
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val a0 = (a + 1.0) + (a - 1.0) * cs + twoSqrtAAlpha
        b0 = a * ((a + 1.0) - (a - 1.0) * cs + twoSqrtAAlpha) / a0
        b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cs) / a0
        b2 = a * ((a + 1.0) - (a - 1.0) * cs - twoSqrtAAlpha) / a0
        a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cs) / a0
        a2 = ((a + 1.0) + (a - 1.0) * cs - twoSqrtAAlpha) / a0
    }

    fun setHighShelf(freq: Double, sampleRate: Double, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * freq / sampleRate
        val sn = sin(omega); val cs = cos(omega)
        val alpha = sn / (2.0 * q.coerceAtLeast(0.01))
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val a0 = (a + 1.0) - (a - 1.0) * cs + twoSqrtAAlpha
        b0 = a * ((a + 1.0) + (a - 1.0) * cs + twoSqrtAAlpha) / a0
        b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cs) / a0
        b2 = a * ((a + 1.0) + (a - 1.0) * cs - twoSqrtAAlpha) / a0
        a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cs) / a0
        a2 = ((a + 1.0) - (a - 1.0) * cs - twoSqrtAAlpha) / a0
    }

    fun process(input: Double): Double {
        val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = input; y2 = y1; y1 = output
        return output
    }
}
