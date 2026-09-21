package com.pure.music.player

import android.content.Context
import com.pure.music.data.Song
import com.pure.music.library.MediaLibraryRepository
import com.pure.music.taglib.TagLibWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 歌词写回内嵌标签服务：通过 TagLib 原生库写 LYRICS 键（并清理旧别名键），
 * 成功后刷新媒体库让新歌词经 StateFlow 回流到 UI。
 */
object LyricsTagService {
    /**
     * 把歌词文本写入 [song] 对应音频文件。
     * @return 失败原因；null 表示写入成功（IO 线程执行）
     */
    suspend fun writeLyrics(context: Context, song: Song, lyricsText: String): String? =
        withContext(Dispatchers.IO) {
            when {
                song.path.isBlank() -> "无法访问音频文件，请确认媒体库包含文件路径"
                !File(song.path).exists() -> "音频文件不存在：${song.path}"
                lyricsText.isBlank() -> "歌词内容为空"
                else -> {
                    val result = TagLibWriter.writeLyrics(song.path, lyricsText)
                    when (result) {
                        is TagLibWriter.Result.Ok -> {
                            // 文件变更会触发 MediaStore 通知，这里再主动刷一次兜底
                            MediaLibraryRepository.get(context.applicationContext).refresh()
                            null
                        }
                        is TagLibWriter.Result.Fail -> "写入标签失败：${result.reason}"
                    }
                }
            }
        }
}
