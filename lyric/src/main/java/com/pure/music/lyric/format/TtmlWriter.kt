package com.pure.music.lyric.format

import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.visibleText

/**
 * 简化版 TTML 写入：输出 Lyrico 可读的 TTML 结构。
 * - 时间用相对毫秒（"123ms"）；逐字输出为 `<span begin/end>`；
 * - 音译/翻译轨用 `ttm:role="x-roman"/"x-translation"`，行关联用 `itunes:key`；
 * - 有逐字时间轴时根节点带 `itunes:timing="Word"`。
 * 重新解析走 [parseTtmlDocument] 可往返（词终点缺省时由下一词起点推得）。
 */
internal fun writeTtml(document: LyricsDocument): String {
    val m = document.metadata
    val rootAttrs = buildList {
        add("xmlns=\"http://www.w3.org/ns/ttml\"")
        add("xmlns:ttm=\"http://www.w3.org/ns/ttml#metadata\"")
        add("xmlns:itunes=\"http://music.apple.com/lyric-ttml-internal\"")
        m.language?.let { add("xml:lang=\"${escapeXml(it)}\"") }
        if (document.hasWordTiming) add("itunes:timing=\"Word\"")
    }.joinToString(" ")

    val head = buildString {
        m.title?.let { append("  <head>\n    <ttm:title>${escapeXml(it)}</ttm:title>\n") }
        m.artist?.let { append("    <ttm:artist>${escapeXml(it)}</ttm:artist>\n") }
        m.album?.let { append("    <ttm:album>${escapeXml(it)}</ttm:album>\n") }
        if (isNotEmpty()) append("  </head>\n")
    }

    val body = buildString {
        append("  <body>\n")
        document.original.forEach { line ->
            appendTtmlLine(this, line)
        }
        document.romanization.forEach { line ->
            appendTtmlCommentLine(this, line, "x-roman")
        }
        document.translation.forEach { line ->
            appendTtmlCommentLine(this, line, "x-translation")
        }
        append("  </body>\n")
    }

    return buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<tt ").append(rootAttrs).append(">\n")
        if (head.isNotEmpty()) append(head)
        append(body).append("</tt>")
    }
}

private fun appendTtmlLine(builder: StringBuilder, line: LyricLine) {
    builder.append("    <p")
    line.startMs?.let { appendTtmlTime(builder, " begin=", it) }
    line.endMs?.let { appendTtmlTime(builder, " end=", it) }
    appendTtmlKey(builder, line)
    builder.append('>').append('\n')
    if (line.words.isEmpty()) {
        builder.append(escapeXml(line.text)).append("</p>\n")
        return
    }
    line.words.forEach { word ->
        if (word.startMs != null) {
            builder.append("      <span")
            appendTtmlTime(builder, " begin=", word.startMs)
            word.endMs?.let { appendTtmlTime(builder, " end=", it) }
            builder.append('>').append(escapeXml(word.text)).append("</span>")
        } else {
            builder.append(escapeXml(word.text))
        }
    }
    builder.append("</p>\n")
}

private fun appendTtmlCommentLine(builder: StringBuilder, line: LyricLine, role: String) {
    builder.append("    <p")
    line.startMs?.let { appendTtmlTime(builder, " begin=", it) }
    line.endMs?.let { appendTtmlTime(builder, " end=", it) }
    builder.append(" ttm:role=\"").append(role).append('"')
    appendTtmlKey(builder, line)
    builder.append(">").append(escapeXml(line.visibleText())).append("</p>\n")
}

private fun appendTtmlTime(builder: StringBuilder, prefix: String, ms: Long) {
    builder.append(prefix).append('"').append(LrcTime.formatTtmlTime(ms)).append('"')
}

private fun appendTtmlKey(builder: StringBuilder, line: LyricLine) {
    line.linkKey?.takeIf { it.isNotBlank() }?.let { builder.append(" itunes:key=\"").append(it).append('"') }
}

/** XML 文本转义 */
internal fun escapeXml(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
