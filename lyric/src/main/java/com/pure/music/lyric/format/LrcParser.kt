package com.pure.music.lyric.format

import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.LyricWord
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.LyricsMetadata

/**
 * LRC 族（普通/逐字/增强）统一解析，规则对齐 Lyrico 的 parseLrc：
 * - 整行 `[key:value]` 标签行（key 字母开头）归入元数据；
 * - 行内首个时间标记为行起点；逐字文本取相邻标记之间的片段；
 * - 增强逐字行的行级 `[t]` 后紧跟 `<w>` 且中间无文本时，行标记不作为词；
 * - 逐字行允许行时间后直接写首字（无标记前缀），该前缀字归属行起点；
 * - 无时间标记的文本行保留（startMs=null），按文档顺序排在时间轴行之前；
 * - 同时间戳多行合并为 主词/音译/翻译：优先含多词的逐字行作主行，
 *   其余行按 ASCII 特征归音译（主行为非 ASCII 时），归不到音译的进翻译轨。
 */
internal fun parseLrcDocument(raw: String, sourceFormat: LyricFormat): LyricsDocument {
    val metadata = mutableMapOf<String, String>()
    val lines = mutableListOf<LyricLine>()

    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach

        val tagMatch = LrcTime.LRC_TAG_LINE_RE.find(trimmed)
        if (tagMatch != null) {
            metadata[tagMatch.groupValues[1]] = tagMatch.groupValues[2].trim()
            return@forEach
        }

        val matches = LrcTime.LRC_TIME_RE.findAll(line).toList()
        if (matches.isEmpty()) {
            if (trimmed.isNotBlank()) lines.add(LyricLine(text = trimmed))
            return@forEach
        }

        val start = LrcTime.lrcTimeMs(matches.first())
        val words = mutableListOf<LyricWord>()
        matches.forEachIndexed { index, match ->
            val next = matches.getOrNull(index + 1)
            val text = line.substring(match.range.last + 1, next?.range?.first ?: line.length)
            // 增强逐字行：行级 [t] 后紧跟 <w> 标记且无文本，行标记只定行起点，不算词
            val isLeadingLineMarker = index == 0 &&
                    match.value.startsWith("[") &&
                    next?.value?.startsWith("<") == true &&
                    text.isBlank()
            if (isLeadingLineMarker || text.isEmpty()) return@forEachIndexed
            val wordStart = LrcTime.lrcTimeMs(match)
            words.add(LyricWord(startMs = wordStart, endMs = next?.let { LrcTime.lrcTimeMs(it) }, text = text))
        }

        if (words.isEmpty()) {
            // 只有行标记、行内无任何文本
            if (line.substringAfterLast("]", "").isNotBlank()) {
                lines.add(LyricLine(startMs = start, text = line.substringAfterLast("]").trim()))
            }
        } else {
            lines.add(
                LyricLine(
                    startMs = start,
                    endMs = words.last().let { LrcTime.wordEndMs(it, null) },
                    text = words.joinToString("") { it.text },
                    words = words
                )
            )
        }
    }

    val (original, romanization, translation) = splitLrcTracks(lines, sourceFormat)
    return LyricsDocument(
        metadata = metadata.toLyricsMetadata(),
        original = original,
        romanization = romanization,
        translation = translation,
        format = sourceFormat
    )
}

/** 无时间轴行在前（保持原文顺序），随后按时间戳分组合并主词/音译/翻译 */
private fun splitLrcTracks(
    lines: List<LyricLine>,
    sourceFormat: LyricFormat
): Triple<List<LyricLine>, List<LyricLine>, List<LyricLine>> {
    val untimed = lines.filter { it.startMs == null }
    val original = untimed.toMutableList()
    val romanization = mutableListOf<LyricLine>()
    val translation = mutableListOf<LyricLine>()

    lines.filter { it.startMs != null }
        .groupBy { it.startMs!! }
        .toSortedMap()
        .forEach { (_, group) ->
            val timedPrimary = group.first { it.startMs != null }
            if (group.size == 1) {
                original.add(timedPrimary.withLinkKey())
            } else {
                // 非普通 LRC 时，同时间的逐字行优先作主行
                val wordLevel = if (sourceFormat == LyricFormat.PLAIN_LRC) {
                    emptyList()
                } else group.filter { it.words.size > 1 }
                val primary = wordLevel.ifEmpty { listOf(timedPrimary) }
                val sub = group.filterNot { it in primary }
                original.addAll(primary.map { it.withLinkKey() })
                if (sub.size >= 2) {
                    val roma = sub.filter { it.text.isMostlyAscii() && primary.any { p -> !p.text.isMostlyAscii() } }
                    romanization.addAll(roma.map { it.withLinkKey(timedPrimary.startMs.toString()) })
                    translation.addAll(sub.filterNot { it in roma }.map { it.withLinkKey(timedPrimary.startMs.toString()) })
                } else {
                    sub.firstOrNull()?.let { line ->
                        if (line.text.isMostlyAscii() && primary.any { !it.text.isMostlyAscii() }) {
                            romanization.add(line.withLinkKey(timedPrimary.startMs.toString()))
                        } else {
                            translation.add(line.withLinkKey(timedPrimary.startMs.toString()))
                        }
                    }
                }
            }
        }

    return Triple(original, romanization, translation)
}

private fun LyricLine.withLinkKey(key: String? = startMs?.toString()): LyricLine =
    copy(linkKey = linkKey ?: key)

private fun Map<String, String>.toLyricsMetadata(): LyricsMetadata {
    val known = setOf("ti", "ar", "al", "offset")
    return LyricsMetadata(
        title = this["ti"],
        artist = this["ar"],
        album = this["al"],
        offsetMs = this["offset"]?.toLongOrNull() ?: 0L,
        extra = filterKeys { it !in known }
    )
}

/** 与旧实现一致：70% 以上为 ASCII 字符视为音译候选 */
private fun String.isMostlyAscii(): Boolean {
    val letters = count { !it.isWhitespace() }
    return letters > 0 && count { it.code < 128 } * 10 >= letters * 7
}
