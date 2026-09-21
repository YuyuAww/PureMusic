package com.pure.music.lyric

import com.pure.music.lyric.model.LyricFormat
import com.pure.music.lyric.model.visibleText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsCodecTest {

    private val plainLrc = """
        [ti:测试]
        [ar:歌手]
        [al:专辑]
        [00:01.000]第一句
        [00:02.500]第二句
        无时间轴行
    """.trimIndent()

    private val enhancedLrc = """
        [00:01.000]<00:01.000>你<00:01.200>好
        [00:02.000]你<00:02.100>好
    """.trimIndent()

    private val verbatimLrc = """
        [00:01.000]你[00:01.200]好
        [00:02.000]天[00:02.150]天[00:02.300]蓝
    """.trimIndent()

    private val ttml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
            xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xml:lang="zh-Hans" itunes:timing="Word">
          <head>
            <ttm:title>测试</ttm:title>
            <ttm:artist>歌手</ttm:artist>
          </head>
          <body>
            <p begin="1000ms" end="1700ms" itunes:key="1000">
              <span begin="1000ms" end="1200ms">你</span><span begin="1200ms">好</span>
            </p>
            <p begin="1000ms" itunes:key="1000" ttm:role="x-roman">ni hao</p>
            <p begin="2000ms" end="3000ms">
              <span begin="2000ms" end="2500ms">天</span><span begin="2500ms">天蓝</span>
            </p>
          </body>
        </tt>
    """.trimIndent()

    // -------------------------------------------------
    // 格式检测
    // -------------------------------------------------
    @Test
    fun `detectFormat 识别 LRC 三种写法与 TTML`() {
        assertEquals(LyricFormat.PLAIN_LRC, LyricsCodec.detectFormat(plainLrc))
        assertEquals(LyricFormat.ENHANCED_LRC, LyricsCodec.detectFormat(enhancedLrc))
        assertEquals(LyricFormat.VERBATIM_LRC, LyricsCodec.detectFormat(verbatimLrc))
        assertEquals(LyricFormat.TTML, LyricsCodec.detectFormat(ttml))
        // 纯文本/空白/带 LRC 标签的无时间文本
        assertNull(LyricsCodec.detectFormat("普通歌词第一行\n普通歌词第二行"))
        assertNull(LyricsCodec.detectFormat("[ti:歌名]\n没有时间的歌词"))
        assertNull(LyricsCodec.detectFormat("   "))
        assertNull(LyricsCodec.detectFormat(null))
        // TTML 判定只看 <tt> 根元素，正文出现 "<tt" 不误判
        assertNull(LyricsCodec.detectFormat("正文提到 <tt 字样"))
        // 带 LRC 时间标记的行优先判为 LRC（正文中的 <tt 字样不参与判定）
        assertEquals(LyricFormat.PLAIN_LRC, LyricsCodec.detectFormat("[00:01.000]正文提到 <tt 字样"))
    }

    // -------------------------------------------------
    // LRC 解析
    // -------------------------------------------------
    @Test
    fun `parse 普通 LRC 提取元数据行与无时间轴行`() {
        val doc = LyricsCodec.parse(plainLrc)!!
        assertEquals(LyricFormat.PLAIN_LRC, doc.format)
        assertEquals("测试", doc.metadata.title)
        assertEquals("歌手", doc.metadata.artist)
        val timed = doc.original.filter { it.startMs != null }
        assertEquals(2, timed.size)
        assertEquals(1000L, timed[0].startMs)
        assertEquals("第一句", timed[0].visibleText())
        // 无时间轴行保留
        assertEquals(1, doc.original.filter { it.startMs == null }.size)
        assertFalse(doc.hasWordTiming)
    }

    @Test
    fun `parse 增强逐字行 行标记后紧跟逐字标记不作为词`() {
        val doc = LyricsCodec.parse(enhancedLrc)!!
        val line = doc.original[0]
        // [00:01.000] 与 <00:01.000> 相邻且无文本：行标记被跳过，"你" 成为首词
        assertEquals(2, line.words.size)
        assertEquals("你", line.words[0].text)
        assertEquals(1000L, line.words[0].startMs)
        assertEquals(1200L, line.words[0].endMs)
        assertEquals("你好", line.visibleText())
        assertTrue(doc.hasWordTiming)
        // 行首无前缀字写法：[00:02.000] 后直接 "你"，首词时间 = 行时间
        val second = doc.original[1]
        assertEquals(2000L, second.words[0].startMs)
        assertEquals(2100L, second.words[0].endMs)
    }

    @Test
    fun `parse 逐字 LRC 行首标记为行起点 首个词时间相同`() {
        val doc = LyricsCodec.parse(verbatimLrc)!!
        val line = doc.original[0]
        assertEquals(1000L, line.startMs)
        assertEquals(2, line.words.size)
        assertEquals("你", line.words[0].text)
        assertEquals(1200L, line.words[0].endMs)
        assertEquals(3, doc.original[1].words.size)
        assertEquals("天天蓝", doc.original[1].visibleText())
    }

    @Test
    fun `parse 同时间戳行合并主词音译与翻译`() {
        val raw = """
            [00:01.000]你好
            [00:01.000]ni hao
            [00:01.000]问候
            [00:02.000]世界
        """.trimIndent()
        val doc = LyricsCodec.parse(raw)!!
        assertEquals(2, doc.original.size)
        assertEquals(listOf("ni hao"), doc.romanization.map { it.text })
        assertEquals(listOf("问候"), doc.translation.map { it.text })
        assertEquals("1000", doc.romanization[0].linkKey)
    }

    // -------------------------------------------------
    // TTML 解析
    // -------------------------------------------------
    @Test
    fun `parse TTML 提取逐字词 子轨 与元数据`() {
        val doc = LyricsCodec.parse(ttml)!!
        assertEquals(LyricFormat.TTML, doc.format)
        assertEquals("zh-Hans", doc.metadata.language)
        assertEquals("测试", doc.metadata.title)
        val line = doc.original[0]
        assertEquals(1000L, line.startMs)
        assertEquals(1700L, line.endMs)
        assertEquals("1000", line.linkKey)
        assertEquals(2, line.words.size)
        assertEquals("你", line.words[0].text)
        assertEquals(1200L, line.words[0].endMs)
        assertEquals("你好", line.visibleText())
        // 子轨
        assertEquals(listOf("ni hao"), doc.romanization.map { it.text })
        assertEquals("1000", doc.romanization[0].linkKey)
        // 第二行逐字
        assertEquals("天天蓝", doc.original[1].visibleText())
        assertNull(doc.translation.firstOrNull())
    }

    @Test
    fun `parse TTML 支持时钟值与秒级时间表达式`() {
        val raw = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <p begin="00:00:01.500">甲</p>
                <p begin="2.25s">乙</p>
                <p begin="45ms">丙</p>
              </body>
            </tt>
        """.trimIndent()
        val doc = LyricsCodec.parse(raw)!!
        assertEquals(listOf(1500L, 2250L, 45L), doc.original.map { it.startMs })
    }

    @Test
    fun `parse 非法内容安全返回 null`() {
        assertNull(LyricsCodec.parse(null))
        assertNull(LyricsCodec.parse("   "))
        // 非法 XML
        assertNull(LyricsCodec.parse("<tt><body><p begin=\"0ms\">未闭合"))
        // DOCTYPE 拒绝
        assertNull(LyricsCodec.parse("<!DOCTYPE x>\n<tt><body></body></tt>"))
    }

    // -------------------------------------------------
    // 编码/转换
    // -------------------------------------------------
    @Test
    fun `encode 逐字格式互转保持时间轴`() {
        val fromVerbatim = LyricsCodec.parse(verbatimLrc)!!
        val enhanced = LyricsCodec.encode(fromVerbatim, LyricFormat.ENHANCED_LRC)!!
        val line = enhanced.lines().first { it.contains("你") }
        assertTrue(line.startsWith("[00:01.000]"))
        assertTrue(line.contains("<00:01.000>你"))
        assertTrue(line.contains("<00:01.200>好"))

        val backVerbatim = LyricsCodec.encode(
            LyricsCodec.parse(enhanced)!!, LyricFormat.VERBATIM_LRC
        )!!
        val reparsed = LyricsCodec.parse(backVerbatim)!!
        assertEquals(
            listOf(1000L to "你", 1200L to "好"),
            reparsed.original[0].words.map { it.startMs to it.text }
        )
    }

    @Test
    fun `encode 普通 LRC 与逐字互转`() {
        val enhancedDoc = LyricsCodec.parse(enhancedLrc)!!
        val plain = LyricsCodec.encode(enhancedDoc, LyricFormat.PLAIN_LRC)!!
        assertTrue(plain.contains("[00:01.000]你好"))
        assertFalse(plain.contains("<00:01.200>"))

        val ttmlOut = LyricsCodec.encode(enhancedDoc, LyricFormat.TTML)!!
        assertTrue(ttmlOut.contains("itunes:timing=\"Word\""))
        val ttmlDoc = LyricsCodec.parse(ttmlOut)!!
        assertEquals(listOf(1000L, 1200L), ttmlDoc.original[0].words.map { it.startMs })
    }

    @Test
    fun `encode 无逐字时间轴不能升格`() {
        val plainDoc = LyricsCodec.parse(plainLrc)!!
        assertNull(LyricsCodec.encode(plainDoc, LyricFormat.ENHANCED_LRC))
        assertNull(LyricsCodec.encode(plainDoc, LyricFormat.TTML))
        assertNotNull(LyricsCodec.encode(plainDoc, LyricFormat.PLAIN_LRC))
    }

    // -------------------------------------------------
    // 偏移
    // -------------------------------------------------
    @Test
    fun `shiftText LRC 与 TTML 偏移 不小于 0`() {
        assertEquals(
            "[00:00.000]你[00:00.150]好",
            LyricsCodec.shiftText("[00:01.000]你[00:01.200]好", -1500L)
        )
        assertEquals(
            "<p begin=\"00:00:00.000\">甲</p>",
            LyricsCodec.shiftText("<p begin=\"00:00:01.000\">甲</p>", -2000L)
        )
        assertEquals("[00:02.000]第一句", LyricsCodec.shiftText("[00:01.000]第一句", 1000L))
    }

    @Test
    fun `shiftDocument 行词同步偏移`() {
        val doc = LyricsCodec.parse(enhancedLrc)!!
        val shifted = LyricsCodec.shiftDocument(doc, 500L)
        assertEquals(1500L, shifted.original[0].startMs)
        assertEquals(1500L, shifted.original[0].words[0].startMs)
        assertEquals(1700L, shifted.original[0].words[0].endMs)
        // 偏移为 0 时原样返回
        assertEquals(doc, LyricsCodec.shiftDocument(doc, 0L))
    }

    // -------------------------------------------------
    // 纯文本输出 / 清理
    // -------------------------------------------------
    @Test
    fun `toPlainText 音译主词翻译顺序 与开关`() {
        val raw = """
            [00:01.000]你好
            [00:01.000]ni hao
            [00:01.000]问候
        """.trimIndent()
        val doc = LyricsCodec.parse(raw)!!
        assertEquals("ni hao\n你好\n问候", LyricsCodec.toPlainText(doc))
        assertEquals("你好", LyricsCodec.toPlainText(doc, false, false))
    }

    @Test
    fun `cleanText 移除占位行与关键词标签行`() {
        val raw = "[00:01.000]/\n[00:02.000]第一句\n[00:03.000]   \n[00:04.000]第二句\n"
        assertEquals(
            "[00:02.000]第一句\n[00:04.000]第二句",
            LyricsCodec.cleanText(raw)
        )
        val noIntro = LyricsCodec.cleanText("[00:05.000]intro\n[00:06.000]走", tagLineKeywords = listOf("intro"))
        assertEquals("[00:06.000]走", noIntro)
    }
}
