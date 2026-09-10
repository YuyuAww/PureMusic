package com.pure.music.ui.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CoverColors(
    val accent: Color,
    val muted: Color,
    val background: Color,
    val surface: Color
)

suspend fun loadCoverColors(context: Context, albumId: Long, fallback: CoverColors): CoverColors = withContext(Dispatchers.IO) {
    runCatching {
        val uri = android.net.Uri.parse("content://media/external/audio/albumart/$albumId")
        val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: return@runCatching fallback
        if (bitmap.width == 0 || bitmap.height == 0) return@runCatching fallback

        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        val stepX = (bitmap.width / 32).coerceAtLeast(1)
        val stepY = (bitmap.height / 32).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val color = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(color) < 128) continue
                red += android.graphics.Color.red(color)
                green += android.graphics.Color.green(color)
                blue += android.graphics.Color.blue(color)
                count++
            }
        }
        bitmap.recycle()
        if (count == 0L) return@runCatching fallback
        val base = android.graphics.Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base, hsl)
        hsl[1] = hsl[1].coerceIn(0.28f, 0.72f)
        hsl[2] = hsl[2].coerceIn(0.30f, 0.52f)
        val accentInt = ColorUtils.HSLToColor(hsl)
        hsl[2] = (hsl[2] + 0.20f).coerceAtMost(0.82f)
        val backgroundInt = ColorUtils.HSLToColor(hsl)
        hsl[2] = 0.96f
        val surfaceInt = ColorUtils.HSLToColor(hsl)
        CoverColors(Color(accentInt), Color(ColorUtils.setAlphaComponent(accentInt, 190)), Color(backgroundInt), Color(surfaceInt))
    }.getOrDefault(fallback)
}
