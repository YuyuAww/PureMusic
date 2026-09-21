package com.pure.music.lyric.format

import com.pure.music.lyric.model.LyricWord

/**
 * LRC 系时间工具。正则对齐 Lyrico 的 LRC_TIME_PATTERN，并放宽两处兼容本地旧歌词：
 * 分数部分可省略（[01:23]），分隔符允许 `.` 或 `:`，分钟允许 1-3 位。
 */
object LrcTime {
    // LRC 时间戳: [01:23.456] 或 <01:23.45>
    val LRC_TIME_RE = Regex("([<\\[])(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?[>\\]]")

    /** LRC 元数据标签行：整行为 [key:value]，key 为字母开头（时间戳行首是数字，不会误判） */
    val LRC_TAG_LINE_RE = Regex("^\\[([A-Za-z][A-Za-z0-9_-]*):(.*)]$")

    // TTML 时钟值: begin="00:01:23.456" / end="01:23.456"
    val TTML_CLOCK_RE = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2})(?:[.:](\\d{1,3}))?)?")

    /** LRC 时间戳格式化为 mm:ss.SSS（mm 可超两位） */
    fun formatLrcTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        return String.format("%02d:%02d.%03d", safe / 60_000, (safe % 60_000) / 1000, safe % 1000)
    }

    /** TTML 时间值（相对时间，毫秒） */
    fun formatTtmlTime(ms: Long): String = "${ms.coerceAtLeast(0L)}ms"

    /** 解析 LRC 时间标记为毫秒；分数不足 3 位时补 0（.12 -> 120ms） */
    fun lrcTimeMs(match: MatchResult): Long {
        val min = match.groupValues[2].toLong()
        val sec = match.groupValues[3].toLong()
        val frac = match.groupValues[4].padEnd(3, '0').take(3).toLong()
        return (min * 60 + sec) * 1000 + frac
    }

    /**
     * 解析 TTML 时间表达式为毫秒，支持（对齐 Lyrico TtmlTime）：
     * 相对时间 123ms / 12.5s / 纯数字（秒）；时钟值 HH:mm:ss[.fff] / mm:ss[.fff]。
     */
    fun ttmlTimeMs(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            text.endsWith("ms", ignoreCase = true) ->
                text.dropLast(2).toLongOrNull()

            text.endsWith("s", ignoreCase = true) && !text.endsWith("ms", ignoreCase = true) ->
                ((text.dropLast(1).toDoubleOrNull() ?: return null) * 1000).toLong()

            else -> {
                // 纯数字（整数/小数）按秒解释（对齐 Lyrico TtmlTime 的 plainSeconds 规则）
                if (text.matches(Regex("^\\d+(?:\\.\\d+)?$"))) {
                    return ((text.toDoubleOrNull() ?: 0.0) * 1000).toLong()
                }
                val m = TTML_CLOCK_RE.matchEntire(text) ?: return null
                val frac = m.groupValues[4].ifEmpty { "0" }.padEnd(3, '0').take(3).toLong()
                val sec = m.groupValues[3].ifEmpty { "0" }.toLong()
                val min = m.groupValues[2].toLong()
                val hour = m.groupValues[1].toLong()
                ((hour * 60 + min) * 60 + sec) * 1000 + frac
            }
        }
    }

    /** LRC 无 end 时间时末词默认时长（与 Lyrico 一致） */
    const val DEFAULT_WORD_DURATION_MS = 500L

    /** 补全词时间：LRC 的 end 缺省为下一词起点，末词为 start + [DEFAULT_WORD_DURATION_MS] */
    fun wordEndMs(word: LyricWord, nextStartMs: Long?): Long? =
        word.endMs ?: nextStartMs ?: (word.startMs?.plus(DEFAULT_WORD_DURATION_MS))
}
