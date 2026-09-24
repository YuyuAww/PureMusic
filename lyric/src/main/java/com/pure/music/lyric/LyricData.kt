package com.pure.music.lyric

import com.pure.music.lyric.model.LyricsDocument

/**
 * 歌词统一载体:原始文本 + 解析后的结构化文档,由 lyric 模块解析、缓存、中转分发。
 *
 * taglib 子模块负责歌词原始文本的读取/写入,本类拿到原始文本后统一解析为
 * [LyricsDocument] 并按歌曲缓存;全项目通过 [of] 取用,不再各自调用
 * [LyricsCodec.parse],避免同一首歌在多个 UI 节点重复解析。
 *
 * 职责:
 * - 解析:委托 [LyricsCodec] 检测格式并解析;
 * - 缓存:按 songId 记录上次文本与结果,文本未变则复用,变更自动重解析;
 * - 中转分发:统一入口,屏蔽格式检测细节。
 */
data class LyricData(
    /** 原始歌词文本(来自 taglib,写回音频文件时直接使用) */
    val rawText: String?,
    /** 解析后的结构化文档;无歌词或解析失败为 null */
    val document: LyricsDocument?
) {
    /** 是否含逐字时间轴 */
    val hasWordTiming: Boolean get() = document?.hasWordTiming == true

    companion object {
        private const val MAX_ENTRIES = 256

        private class Entry(val rawText: String?, val data: LyricData)

        // 访问顺序 LRU,超容量淘汰最旧
        private val cache = object : LinkedHashMap<Long, Entry>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Entry>?): Boolean = size > MAX_ENTRIES
        }
        private val lock = Any()

        /**
         * 获取歌曲歌词载体。文本与缓存一致则复用解析结果,否则重新解析。
         * 同一首歌在显示轨、详情、编辑面板等处重复调用只解析一次。
         */
        fun of(songId: Long, rawText: String?): LyricData = synchronized(lock) {
            val cached = cache[songId]
            if (cached != null && cached.rawText == rawText) return@synchronized cached.data
            val data = LyricData(rawText, LyricsCodec.parse(rawText))
            cache[songId] = Entry(rawText, data)
            data
        }
    }
}
