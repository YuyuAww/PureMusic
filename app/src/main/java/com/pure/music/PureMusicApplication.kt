package com.pure.music

import android.app.Application
import com.pure.music.memory.FairMemoryManager
import com.pure.music.ui.component.library.trimArtworkMemoryCache
import com.pure.music.ui.component.playback.trimBlurredArtworkMemoryCache

class PureMusicApplication : Application() {
    internal lateinit var fairMemoryManager: FairMemoryManager
        private set

    override fun onCreate() {
        super.onCreate()
        fairMemoryManager = FairMemoryManager(::trimImageCaches)
        fairMemoryManager.register(this)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimImageCaches()
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        trimImageCaches()
    }

    private fun trimImageCaches() {
        trimArtworkMemoryCache()
        trimBlurredArtworkMemoryCache()
    }
}
