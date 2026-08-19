package com.ella.music.viewmodel

import com.ella.music.data.SettingsManager
import kotlin.math.pow

internal fun Float?.toReplayGainVolume(): Float {
    val gainDb = this?.coerceIn(-24f, 0f) ?: return 1f
    return 10f.pow(gainDb / 20f).coerceIn(0.05f, 1f)
}

internal fun shouldReplayFromPreviousButton(
    manualSeekAfterPreviousButton: Boolean,
    previousButtonAction: Int,
    currentPositionMs: Long
): Boolean =
    !manualSeekAfterPreviousButton &&
        previousButtonAction == SettingsManager.PREVIOUS_BUTTON_REPLAY_CURRENT &&
        currentPositionMs >= SettingsManager.PREVIOUS_REPLAY_THRESHOLD_MS