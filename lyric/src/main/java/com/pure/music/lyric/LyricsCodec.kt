package com.pure.music.lyric

import com.pure.music.lyric.format.LrcTime
import com.pure.music.lyric.format.parseLrcDocument
import com.pure.music.lyric.format.parseTtmlDocument
import com.pure.music.lyric.format.writeLrc
import com.pure.music.lyric.format.writeTtml
import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.LyricWord
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.visibleText

/**
 * 歌词编解码统一入口（架构参照 Lyrico 的 LyricDecoder/LyricEncoder/LyricsDocumentPipeline）：
 * 检测格式 → 解析为 [LyricsDocument] → 按需转换/偏移/输出。
 * 歌词来自音频内嵌标签等不可信来源，解析异常一律返回 null，不向外抛。
 */
object LyricsCodec {

    /** 检测歌词文本格式；无时间轴的纯文本与空内容返回 null */
    fun detectFormat(raw: String?): LyricFormat? {
        val text = raw?.trimStart('\uFEFF')?.trimStart() ?: return null
        if (text.isBlank()) return null
        if (looksLikeTtml(text)) return LyricFormat.TTML

        // 抽样扫描：一行内"方括号行时间 + 尖括号逐字时间"即增强逐字；
        // 多个方括号标记为逐字 LRC；单个方括号为普通 LRC；只有尖括号的行按逐字处理。
        var hasEnhanced = false
        var hasVerbatim = false
        var hasPlain = false
        for (line in text.lines()) {
            if (line.isBlank()) continue
            if (LrcTime.LRC_TAG_LINE_RE.find(line) != null) continue
            val matches = LrcTime.LRC_TIME_RE.findAll(line).toList()
            if (matches.isEmpty()) continue
            val hasBracket = matches.any { it.value.startsWith("[") }
            val hasAngle = matches.any { it.value.startsWith("<") }
            when {
                hasBracket && hasAngle -> { hasEnhanced = true; break }
                hasBracket && matches.size > 1 -> { hasVerbatim = true; break }
                !hasBracket && hasAngle -> { hasEnhanced = true; break }
                hasBracket -> hasPlain = true
            }
        }
        return when {
            hasEnhanced -> LyricFormat.ENHANCED_LRC
            hasVerbatim -> LyricFormat.VERBATIM_LRC
            hasPlain -> LyricFormat.PLAIN_LRC
            else -> null
        }
    }

    /**
     * 安全解析入口：
     * - LRC/ELRC/TTML 按检测出的格式解析；
     * - 无时间轴的非空文本包装为纯文本文档（startMs=null）；
     * - 空内容返回 null，解析异常返回 null（调用方展示"暂无歌词"）。
     */
    fun parse(raw: String?): LyricsDocument? = runCatching {
        val text = raw?.trimStart('\uFEFF')?.trim() ?: return@runCatching null
        if (text.isBlank()) return@runCatching null
        val format = detectFormat(text)
        when (format) {
            LyricFormat.PLAIN_LRC, LyricFormat.VERBATIM_LRC, LyricFormat.ENHANCED_LRC ->
                parseLrcDocument(text, format)
            LyricFormat.TTML -> parseTtmlDocument(text)
            else -> LyricsDocument(original = plainLines(text))
        }
    }.getOrNull()?.let { doc -> doc.copy(format = detectFormat(raw)) }

    private fun plainLines(text: String): List<LyricLine> =
        text.lines().filter { it.isNotBlank() }.map { LyricLine(text = it.trim()) }

    /**
     * 编码/转换为目标格式文本。返回 null 表示转换不可行：
     * 源无逐字时间轴时不能转换到逐字格式（与 Lyrico 的格式化约束一致）；
     * 普通 LRC/纯文本写出始终可行。
     */
    fun encode(document: LyricsDocument, target: LyricFormat = document.format ?: LyricFormat.PLAIN_LRC): String? {
        if (target.usesWordTiming && !document.hasWordTiming) return null
        return when (target) {
            LyricFormat.PLAIN_LRC, LyricFormat.VERBATIM_LRC, LyricFormat.ENHANCED_LRC ->
                writeLrc(document, target)
            LyricFormat.TTML -> writeTtml(document)
        }
    }

    /** 纯文本输出：行组内按 音译 → 主词 → 翻译 顺序拼平（对齐 Lyrico 默认内容顺序） */
    fun toPlainText(
        document: LyricsDocument,
        showRomanization: Boolean = true,
        showTranslation: Boolean = true
    ): String {
        if (document.original.isEmpty()) return ""
        val romanBy = document.romanization.filter { it.linkKey != null }.groupBy { it.linkKey!! }
        val transBy = document.translation.filter { it.linkKey != null }.groupBy { it.linkKey!! }
        val builder = StringBuilder()

        // 无时间轴行按原文顺序
        document.original.filter { it.startMs == null }.forEach {
            builder.append(it.visibleText()).append('\n')
        }

        document.original.filter { it.startMs != null }
            .groupBy { it.startMs }
            .toSortedMap()
            .forEach { (start, group) ->
                val key = group.firstOrNull { !it.linkKey.isNullOrBlank() }?.linkKey ?: start.toString()
                if (showRomanization) romanBy[key].orEmpty().forEach {
                    builder.append(it.visibleText()).append('\n')
                }
                group.forEach { builder.append(it.visibleText()).append('\n') }
                if (showTranslation) transBy[key].orEmpty().forEach {
                    builder.append(it.visibleText()).append('\n')
                }
            }
        return builder.toString().trim()
    }

    /**
     * 对原始歌词文本整体做时间偏移（LRC 时间标记 + TTML 时钟值双正则，对齐 Lyrico
     * LyricEncoder.shiftLyricsText）。正数延后、负数提前，结果不小于 0。
     */
    fun shiftText(raw: String, offsetMs: Long): String {
        if (offsetMs == 0L || raw.isBlank()) return raw
        return if (detectFormat(raw) == LyricFormat.TTML) {
            // TTML 时钟值（begin="00:01:23.456" / "01:23.456"），按从后往前替换避免位移
            var result = raw
            LrcTime.TTML_CLOCK_RE.findAll(result).toList()
                .sortedByDescending { it.range.first }
                .forEach { m ->
                    val total = ((LrcTime.ttmlTimeMs(m.value) ?: 0L) + offsetMs).coerceAtLeast(0L)
                    val shifted = String.format("%02d:%02d:%02d.%03d", total / 3_600_000, (total % 3_600_000) / 60_000, (total % 60_000) / 1000, total % 1000)
                    result = result.substring(0, m.range.first) + shifted + result.substring(m.range.last + 1)
                }
            result
        } else {
            LrcTime.LRC_TIME_RE.replace(raw) { m ->
                val total = (LrcTime.lrcTimeMs(m) + offsetMs).coerceAtLeast(0L)
                String.format(
                    "%s%02d:%02d.%03d%s",
                    m.groupValues[1], total / 60_000, (total % 60_000) / 1000, total % 1000, m.groupValues[5]
                )
            }
        }
    }

    /** 文档级偏移：所有行/词时间 +offset（不小于 0），与写回前预览配合使用 */
    fun shiftDocument(document: LyricsDocument, offsetMs: Long): LyricsDocument {
        if (offsetMs == 0L) return document
        val shift: (Long?) -> Long? = { ms -> ms?.let { (it + offsetMs).coerceAtLeast(0L) } }
        val mapLine: (LyricLine) -> LyricLine = { line ->
            line.copy(
                startMs = shift(line.startMs),
                endMs = shift(line.endMs),
                words = line.words.map { LyricWord(shift(it.startMs), shift(it.endMs), it.text) }
            )
        }
        return document.copy(
            original = document.original.map(mapLine),
            romanization = document.romanization.map(mapLine),
            translation = document.translation.map(mapLine)
        )
    }

    /** 清理占位行（纯空白、/ 分隔线等），tagLineKeywords 命中的 LRC 标签行一并移除 */
    fun cleanText(raw: String, removeEmptyLines: Boolean = true, tagLineKeywords: List<String> = emptyList()): String {
        val keywords = tagLineKeywords.map { it.trim() }.filter { it.isNotEmpty() }
        return raw.lines()
            .filterNot { line ->
                val visible = line.replace(LrcTime.LRC_TIME_RE, "").replace(Regex("<[^>]+>"), "").trim()
                val removeEmpty = removeEmptyLines &&
                        (visible.isEmpty() || visible.matches(Regex("^[\\s/\\\\|｜·・.。…_-]*$")))
                val removeTag = keywords.any { line.contains(it, ignoreCase = true) }
                removeEmpty || removeTag
            }
            .joinToString("\n")
            .trim()
    }

    /** 判断文本是否以 TTML 根元素 `<tt>` 开头（容忍 BOM/XML 声明/注释，对齐 Lyrico） */
    private fun looksLikeTtml(text: String): Boolean {
        var rest = text.trimStart()
        while (true) {
            rest = when {
                rest.startsWith("<?") -> {
                    val end = rest.indexOf("?>", startIndex = 2)
                    if (end < 0) return false
                    rest.substring(end + 2).trimStart()
                }
                rest.startsWith("<!--") -> {
                    val end = rest.indexOf("-->", startIndex = 4)
                    if (end < 0) return false
                    rest.substring(end + 3).trimStart()
                }
                else -> {
                    if (!rest.startsWith("<tt")) return false
                    val boundary = rest.getOrNull(3) ?: return false
                    return boundary == '>' || boundary == '/' || boundary.isWhitespace()
                }
            }
        }
    }
}

