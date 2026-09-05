package com.pure.music.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pure.music.library.MediaLibraryRepository

/** 定时媒体库扫描 Worker，每日执行一次刷新 MediaStore 缓存 */
class LibraryScanWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        try {
            val repository = MediaLibraryRepository.get(applicationContext)
            repository.refresh()
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
