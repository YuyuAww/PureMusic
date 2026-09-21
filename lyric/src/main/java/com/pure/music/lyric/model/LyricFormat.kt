package com.pure.music.lyric.model

/**
 * 歌词格式，参照 Lyrico 的四格式划分：
 * - [PLAIN_LRC]：每行一个 `[mm:ss.xx]` 行时间标记的普通 LRC；
 * - [VERBATIM_LRC]：同一行多个 `[mm:ss.xx]` 标记，逐字时间轴（纯方括号写法）；
 * - [ENHANCED_LRC]：行级 `[mm:ss.xx]` + 逐字 `<mm:ss.xx>` 混合的增强逐字歌词；
 * - [TTML]：`<tt>` XML 文档（含逐字 span 时间轴）。
 * 无法识别时间轴格式时为纯文本歌词，由 [com.pure.music.lyric.LyricsCodec.parse] 直接包装。
 */
enum class LyricFormat {
    PLAIN_LRC,
    VERBATIM_LRC,
    ENHANCED_LRC,
    TTML;

    /** 目标格式是否需要源数据提供逐字时间轴（无逐字时间轴的源不能转换到这些格式） */
    val usesWordTiming: Boolean
        get() = this == VERBATIM_LRC || this == ENHANCED_LRC || this == TTML

    /** 展示用标签 */
    fun label(): String = when (this) {
        PLAIN_LRC -> "普通 LRC"
        VERBATIM_LRC -> "逐字 LRC"
        ENHANCED_LRC -> "增强逐字"
        TTML -> "TTML"
    }
}
