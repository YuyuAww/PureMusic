package com.pure.music.lyric.format

import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.visibleText

/**
 * LRC 族写入（普通/逐字/增强），规则对齐 Lyrico 的三个 LRC writer：
 * - 元数据标签行在前；行组按 startMs 排序（无时间轴行保留为纯文本行）；
 * - 每组依次输出 主词行 → 音译行 → 翻译行（固定顺序，对齐 Lyrico 默认 原词/音译/翻译）；
 * - 逐字标记只输出词起点，不伪造词终点（LRC 无 end 概念，终点由下一词起点推得）。
 */
internal fun writeLrc(document: LyricsDocument, target: LyricFormat): String {
    val builder = StringBuilder()
    appendLrcTags(builder, document)

    val lines = document.original
    if (lines.isEmpty()) return builder.toString().trim()
    val isWordLevel = target != LyricFormat.PLAIN_LRC && lines.any { it.words.size > 1 }

    // 无时间轴行先输出（保持原文顺序）
    lines.filter { it.startMs == null }.forEach { line ->
        builder.append(line.visibleText()).append('\n')
    }

    val romanBy = document.romanization.filter { it.linkKey != null }.groupBy { it.linkKey!! }
    val translationBy = document.translation.filter { it.linkKey != null }.groupBy { it.linkKey!! }

    lines.filter { it.startMs != null }
        .groupBy { it.startMs }
        .toSortedMap()
        .forEach { (start, group) ->
            group.forEach { line -> appendOriginalLine(builder, line, target, isWordLevel) }
            val key = lineKey(start, group)
            romanBy[key].orEmpty().forEach { appendTimedLine(builder, it, start) }
            translationBy[key].orEmpty().forEach { appendTimedLine(builder, it, start) }
        }
    return builder.toString().trim()
}

private fun lineKey(startMs: Long?, group: List<LyricLine>): String {
    return group.firstOrNull { !it.linkKey.isNullOrBlank() }?.linkKey ?: startMs?.toString() ?: ""
}

private fun appendLrcTags(builder: StringBuilder, document: LyricsDocument) {
    val m = document.metadata
    m.title?.let { builder.append("[ti:").append(it).append("]\n") }
    m.artist?.let { builder.append("[ar:").append(it).append("]\n") }
    m.album?.let { builder.append("[al:").append(it).append("]\n") }
    if (m.offsetMs != 0L) builder.append("[offset:").append(m.offsetMs).append("]\n")
    m.extra.forEach { (key, value) ->
        builder.append('[').append(key).append(':').append(value).append("]\n")
    }
}

/** 主词行按目标格式输出 */
private fun appendOriginalLine(
    builder: StringBuilder,
    line: LyricLine,
    target: LyricFormat,
    isWordLevel: Boolean
) {
    if (!isWordLevel || line.words.size <= 1) {
        appendTimedLine(builder, line, line.startMs)
        return
    }
    when (target) {
        LyricFormat.ENHANCED_LRC -> {
            // 行级 [t] + 逐字 <w>；行起点与首词起点相同时行标记与首词标记相邻且无文本，
            // 解析端会正确跳过行标记（对齐 Lyrico 的 isLeadingEnhancedLineTimestamp 规则）
            line.startMs?.let { builder.append('[').append(LrcTime.formatLrcTime(it)).append(']') }
            line.words.forEach { word ->
                word.startMs?.let { builder.append('<').append(LrcTime.formatLrcTime(it)).append('>') }
                builder.append(word.text)
            }
            builder.append('\n')
        }

        LyricFormat.VERBATIM_LRC -> {
            line.words.forEach { word ->
                word.startMs?.let { builder.append('[').append(LrcTime.formatLrcTime(it)).append(']') }
                builder.append(word.text)
            }
            builder.append('\n')
        }

        else -> appendTimedLine(builder, line, line.startMs)
    }
}

/** 音译/翻译行与降级为普通 LRC 的行：[t]文本 */
private fun appendTimedLine(builder: StringBuilder, line: LyricLine, fallbackStartMs: Long?) {
    val start = line.startMs ?: fallbackStartMs ?: return
    builder.append('[')
        .append(LrcTime.formatLrcTime(start))
        .append(']')
        .append(line.visibleText())
        .append('\n')
}
