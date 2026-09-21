package com.pure.music.lyric.model

/** 歌词内嵌元数据（LRC 的 [ti:]/[ar:]/[al:]/[offset:] 标签行，或 TTML head） */
data class LyricsMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val offsetMs: Long = 0L,
    val language: String? = null,
    val extra: Map<String, String> = emptyMap()
)

/**
 * 一行歌词。[startMs] 为 null 表示无时间轴（纯文本行或无时间标记行）。
 * 有逐字时间轴时 [words] 非空，行文本可由词拼接得出。
 * [linkKey] 为同时间戳多轨行（主词/音译/翻译）的关联键，通常为 startMs 字符串。
 */
data class LyricLine(
    val startMs: Long? = null,
    val endMs: Long? = null,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val linkKey: String? = null
)

/**
 * 逐字时间点。LRC 系格式只带 start（end 由下一词起点补出，末词缺省 +500ms），
 * TTML 的 span 可带显式 [endMs]。
 */
data class LyricWord(
    val startMs: Long?,
    val endMs: Long?,
    val text: String
)

/**
 * 歌词文档模型（参照 Lyrico 的多轨结构，简化掉 agent/扩展元素）：
 * 原文轨 + 可选音译轨 + 可选翻译轨，同时间戳行通过 [LyricLine.linkKey] 对齐。
 */
data class LyricsDocument(
    val metadata: LyricsMetadata = LyricsMetadata(),
    val original: List<LyricLine>,
    val romanization: List<LyricLine> = emptyList(),
    val translation: List<LyricLine> = emptyList(),
    val format: LyricFormat? = null
) {
    /** 是否含有真正的逐字时间轴（某行 words 数 > 1；单个词只是行时间，不算逐字） */
    val hasWordTiming: Boolean get() = original.any { it.words.size > 1 }

    companion object {
        /** 无任何歌词内容的占位文档（对应界面"暂无内嵌歌词"） */
        val EMPTY = LyricsDocument(original = emptyList())
    }
}

/** 行可见文本：有逐字数据时按词拼接，否则用行文本 */
fun LyricLine.visibleText(): String = if (words.isNotEmpty()) words.joinToString("") { it.text } else text
