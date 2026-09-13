package com.pure.music.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CoverColors(
    val accent: Color,
    val muted: Color,
    val background: Color,
    val surface: Color,
    val gradientStart: Color = background,
    val gradientEnd: Color = surface
)
private val coverCache = ConcurrentHashMap<String, CoverColors>()

suspend fun loadCoverColors(context: Context, albumId: Long, fallback: CoverColors, darkTheme: Boolean = false): CoverColors = withContext(Dispatchers.IO) {
    val key = "$albumId:$darkTheme"
    coverCache[key]?.let { return@withContext it }
    runCatching {
        val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 29) {
            runCatching { context.contentResolver.loadThumbnail(uri, android.util.Size(64, 64), null) }.getOrNull()
                ?: context.contentResolver.openInputStream(uri)?.use { decodeSmall(it) }
                ?: return@runCatching fallback
        } else {
            context.contentResolver.openInputStream(uri)?.use { decodeSmall(it) }
                ?: return@runCatching fallback
        }
        if (bitmap.width == 0 || bitmap.height == 0) return@runCatching fallback

        val bins = LongArray(24 * 6 * 6); val hsv = FloatArray(3)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val color = bitmap.getPixel(x, y); if (AndroidColor.alpha(color) < 128) continue
            AndroidColor.colorToHSV(color, hsv)
            if (hsv[2] < .06f || hsv[2] > .98f && hsv[1] < .12f) continue
            val h = ((hsv[0] / 360f) * 24).toInt().coerceIn(0, 23); val s = (hsv[1] * 6).toInt().coerceIn(0, 5); val v = (hsv[2] * 6).toInt().coerceIn(0, 5)
            bins[(h * 6 + s) * 6 + v]++
        }
        bitmap.recycle()
        val score: (Int) -> Double = { i -> bins[i] * (1.0 + ((i / 6) % 6) / 3.0) }
        val best = bins.indices.maxByOrNull(score)
            ?.takeIf { bins[it] > 0 } ?: return@runCatching fallback
        val second = bins.indices.filter { it != best && bins[it] > 0 }.maxByOrNull(score)
        fun colorOf(i: Int, value: Float? = null): Int = AndroidColor.HSVToColor(floatArrayOf((i / 36 + .5f) * 15f, (((i / 6) % 6) + .5f) / 6f, value ?: ((i % 6) + .5f) / 6f))
        val base = colorOf(best)
        val secondary = colorOf(second ?: best, .55f)
        AndroidColor.colorToHSV(base, hsv); val colorful = hsv[1] >= .10f
        if (colorful) { hsv[1] = hsv[1].coerceIn(.42f, .90f); hsv[2] = hsv[2].coerceIn(.38f, .68f) } else { hsv[1] = 0f; hsv[2] = hsv[2].coerceIn(.35f, .65f) }
        var accentInt = AndroidColor.HSVToColor(hsv)
        val accentHsv = hsv.copyOf()
        // MD3 background/surface are neutral roles: retain only a subtle cover tint.
        hsv[1] *= if (darkTheme) .16f else .10f
        hsv[2] = if (darkTheme) .14f else .96f
        val backgroundInt = AndroidColor.HSVToColor(hsv)
        hsv[2] = if (darkTheme) .20f else .99f
        val surfaceInt = AndroidColor.HSVToColor(hsv)
        if (ColorUtils.calculateContrast(accentInt, backgroundInt) < 3.0) { val adjusted = accentHsv.copyOf(); adjusted[2] = if (darkTheme) .85f else .25f; accentInt = AndroidColor.HSVToColor(adjusted) }
        val mutedBase = ColorUtils.blendARGB(accentInt, backgroundInt, .55f); AndroidColor.colorToHSV(mutedBase, hsv); hsv[1] *= .55f; hsv[2] = if (darkTheme) hsv[2].coerceIn(.45f, .72f) else hsv[2].coerceIn(.30f, .55f)
        val gradientStart = Color(accentInt)
        val gradientEnd = Color(secondary)
        CoverColors(Color(accentInt), Color(AndroidColor.HSVToColor(hsv)), Color(backgroundInt), Color(surfaceInt), gradientStart, gradientEnd).also { coverCache[key] = it }
    }.getOrDefault(fallback)
}

private fun decodeSmall(input: java.io.InputStream): Bitmap? { val bytes = input.readBytes(); val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds); val sample = maxOf(1, Integer.highestOneBit(maxOf(bounds.outWidth, bounds.outHeight) / 64)); return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) }
