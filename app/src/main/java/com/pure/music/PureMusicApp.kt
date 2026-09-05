package com.pure.music

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.pure.music.player.PlayerManager
import com.pure.music.work.LibraryScanWorker
import java.util.concurrent.TimeUnit

/** 应用入口，初始化播放器并注册定时媒体库扫描任务 */
class PureMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayerManager.init(this)
        scheduleLibraryScan()
    }

    /** 注册每日定时扫描任务，电量低时跳过 */
    private fun scheduleLibraryScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequest.Builder(
            LibraryScanWorker::class.java,
            24,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "library_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
