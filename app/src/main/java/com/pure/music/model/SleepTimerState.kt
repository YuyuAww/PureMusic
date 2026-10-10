package com.pure.music.model

data class SleepTimerState(
    val active: Boolean = false,
    val remainingSeconds: Int = 0,
    val extensionSeconds: Int = 0,
    val extending: Boolean = false,
    val interruptionNotice: Int = 0,
)
