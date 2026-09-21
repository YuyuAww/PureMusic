package com.pure.music.taglib

/** 音频内嵌标签写入（TagLib 原生库）。当前只开放歌词写入 */
object TagLibWriter {
    sealed interface Result {
        object Ok : Result
        data class Fail(val reason: String) : Result
    }

    private val loaded: Boolean = runCatching {
        // 与 TagLibMetadataReader 加载同一个库，重复 loadLibrary 为无副作用
        System.loadLibrary("puremusic_taglib")
        true
    }.getOrDefault(false)

    /** 把歌词文本写入音频文件内嵌标签的 LYRICS 键（并清理旧别名键）。IO 线程调用 */
    fun writeLyrics(path: String, lyrics: String): Result = when {
        !loaded -> Result.Fail("原生标签库未加载")
        path.isBlank() -> Result.Fail("音频文件路径为空")
        else -> runCatching {
            if (writeLyricsNative(path, lyrics)) Result.Ok else Result.Fail("标签保存失败（权限不足或格式不支持）")
        }.getOrElse { Result.Fail("写入异常：${it.message ?: it::class.simpleName}") }
    }

    @JvmStatic
    private external fun writeLyricsNative(path: String, lyrics: String): Boolean
}
