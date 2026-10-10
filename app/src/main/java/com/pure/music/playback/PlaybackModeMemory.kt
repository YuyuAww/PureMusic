package com.pure.music.playback

import com.pure.music.model.PlaybackMode

/** Keeps the service-owned playback mode authoritative while queue moves preserve media items. */
internal object PlaybackModeMemory {
    @Volatile
    private var value = PlaybackMode.ORDER

    fun get(): PlaybackMode = value

    fun set(mode: PlaybackMode) {
        value = mode
    }
}
