package com.pure.music.lyric.format

import com.pure.music.lyric.model.LyricLine
import com.pure.music.lyric.model.LyricWord
import com.pure.music.lyric.model.LyricsDocument
import com.pure.music.lyric.model.LyricsMetadata
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * 简化版 TTML 解析（对齐 Lyrico TtmlParser 的核心语义，不处理 agent/扩展元素/ruby）：
 * - `<p begin/end>` 时间 + role 分轨（含 "roman" → 音译，含 "translation" → 翻译，x-bg 背景词跳过）；
 * - 主轨 `<p>` 内带 `begin` 的 `<span>` 为逐字词（`end` 可选）；
 * - 带注释 role 的内嵌 span 文本归入对应子轨；
 * - `itunes:key`（或旧 itunes 命名空间/无命名空间 key）为行关联键，缺失时退回 startMs；
 * - 根节点 xml:lang 记录语言；head 内 ttml:title/artist/album 作元数据。
 * 安全：禁用 DOCTYPE 与外部实体（歌词是内嵌标签里的不可信输入）。
 */
internal fun parseTtmlDocument(raw: String): LyricsDocument {
    require(!raw.contains("<!DOCTYPE", ignoreCase = true)) { "TTML 不支持 DOCTYPE" }
    val doc = newSafeDocumentBuilder().parse(InputSource(StringReader(raw)))
    val root = doc.documentElement
    require(root.localName == "tt") { "不是有效的 TTML 根元素: ${root.tagName}" }

    val language = root.getAttributeNS(NS_XML, "lang").ifBlank {
        root.getAttribute("xml:lang").ifBlank { null }
    }

    val original = mutableListOf<LyricLine>()
    val romanization = mutableListOf<LyricLine>()
    val translation = mutableListOf<LyricLine>()

    root.allElementsWithLocalName("p").forEach { p ->
        val role = p.getAttributeNS(NS_TTM, "role").ifBlank { p.getAttribute("role") }
        val key = p.getAttributeNS(NS_ITUNES, "key").ifBlank {
            p.getAttributeNS(NS_ITUNES_LEGACY, "key").ifBlank { p.getAttribute("key") }
        }
        val parsed = parseP(p, isCommentTrack = role.isNotBlank())
        val lineText = parsed.parts.joinToString("")
        if (parsed.startMs == null && parsed.endMs == null && lineText.isBlank() &&
            parsed.words.isEmpty() && key.isBlank()
        ) return@forEach

        val line = LyricLine(
            startMs = parsed.startMs,
            endMs = parsed.endMs,
            text = lineText,
            words = if (role.isBlank()) parsed.words else emptyList(),
            linkKey = key.ifBlank { parsed.startMs?.toString() }
        )
        when {
            isRomanRole(role) -> romanization.add(line)
            isTranslationRole(role) -> translation.add(line)
            isBackgroundRole(role) -> { /* x-bg 背景词不展示 */ }
            else -> {
                original.add(line)
                parsed.inlineRomanization?.takeIf { it.isNotBlank() }?.let {
                    romanization.add(LyricLine(parsed.startMs, parsed.endMs, it, emptyList(), line.linkKey))
                }
                parsed.inlineTranslation?.takeIf { it.isNotBlank() }?.let {
                    translation.add(LyricLine(parsed.startMs, parsed.endMs, it, emptyList(), line.linkKey))
                }
            }
        }
    }

    return LyricsDocument(
        metadata = LyricsMetadata(
            title = headElementText(root, "title"),
            artist = headElementText(root, "artist"),
            album = headElementText(root, "album"),
            language = language
        ),
        original = original,
        romanization = romanization,
        translation = translation
    )
}

private fun isRomanRole(role: String) = role.contains("roman", true)
private fun isTranslationRole(role: String) = role.contains("translation", true)
private fun isBackgroundRole(role: String) = role == "x-bg" || role == "background"

private data class ParsedP(
    val startMs: Long?,
    val endMs: Long?,
    val parts: List<String>,
    val words: List<LyricWord>,
    val inlineRomanization: String?,
    val inlineTranslation: String?
)

private fun parseP(p: Element, isCommentTrack: Boolean): ParsedP {
    val start = p.timeAttr("begin")
    val end = p.timeAttr("end")
    val parts = mutableListOf<String>()
    val words = mutableListOf<LyricWord>()
    var inlineRoma: StringBuilder? = null
    var inlineTrans: StringBuilder? = null

    fun visit(node: Node) {
        when (node.nodeType) {
            Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                val text = normalize(node.nodeValue)
                if (text.isNotEmpty()) parts.add(text)
            }

            Node.ELEMENT_NODE -> {
                val el = node as Element
                val elRole = el.getAttributeNS(NS_TTM, "role").ifBlank { el.getAttribute("role") }
                when {
                    isRomanRole(elRole) -> {
                        val text = normalize(el.textContent)
                        if (text.isNotBlank()) {
                            val sb = inlineRoma ?: StringBuilder().also { inlineRoma = it }
                            sb.append(text)
                        }
                    }

                    isTranslationRole(elRole) -> {
                        val text = normalize(el.textContent)
                        if (text.isNotBlank()) {
                            val sb = inlineTrans ?: StringBuilder().also { inlineTrans = it }
                            sb.append(text)
                        }
                    }

                    isBackgroundRole(elRole) -> { /* 跳过背景词 */ }

                    // 主轨里带 begin 的 span 为逐字词；注释轨的 span 不作词
                    !isCommentTrack && el.getAttribute("begin").isNotBlank() -> {
                        val text = normalize(el.textContent)
                        if (text.isNotBlank()) {
                            words.add(
                                LyricWord(
                                    startMs = LrcTime.ttmlTimeMs(el.getAttribute("begin")),
                                    endMs = el.timeAttr("end"),
                                    text = text
                                )
                            )
                        }
                    }

                    else -> el.childNodes.forEach { visit(it) }
                }
            }
        }
    }
    p.childNodes.forEach { visit(it) }
    return ParsedP(start, end, parts, words, inlineRoma?.toString(), inlineTrans?.toString())
}

private fun Element.timeAttr(name: String): Long? {
    val value = getAttribute(name)
    return if (value.isBlank()) null else LrcTime.ttmlTimeMs(value)
}

/** 折叠空白 */
private fun normalize(text: String?): String =
    (text ?: "").replace(Regex("\\s+"), " ").trim()

private fun newSafeDocumentBuilder() =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        setFeatureSafe("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeatureSafe("http://xml.org/sax/features/external-general-entities", false)
        setFeatureSafe("http://xml.org/sax/features/external-parameter-entities", false)
        setFeatureSafe("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }.newDocumentBuilder()

private fun DocumentBuilderFactory.setFeatureSafe(uri: String, value: Boolean) {
    runCatching { setFeature(uri, value) }
}

/** 按 localName 递归收集元素（文档序） */
private fun Element.allElementsWithLocalName(local: String): List<Element> {
    val result = mutableListOf<Element>()
    fun visit(node: Node) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val el = node as Element
            if (el.localName == local) result.add(el)
            el.childNodes.forEach { visit(it) }
        }
    }
    childNodes.forEach { visit(it) }
    return result
}

private fun headElementText(root: Element, name: String): String? {
    val head = root.allElementsWithLocalName("head").firstOrNull() ?: return null
    return head.allElementsWithLocalName(name).firstOrNull()
        ?.let { normalize(it.textContent) }
        ?.ifBlank { null }
}

private const val NS_TTM = "http://www.w3.org/ns/ttml#metadata"
private const val NS_ITUNES = "http://music.apple.com/lyric-ttml-internal"
private const val NS_ITUNES_LEGACY = "http://music.apple.com/itunes/ttml"
private const val NS_XML = "http://www.w3.org/XML/1998/namespace"

