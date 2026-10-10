package com.pure.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 品牌强调色：封面取色失败时作为 Miuix 动态色种子回退。 */
val BrandAccent = Color(0xFF6750A4)

/** 封面取色失败时使用的回退 CoverColors（仅用于判定取色是否成功）。 */
private val brandFallbackColors = CoverColors(
    accent = BrandAccent,
    muted = Color(0xFF5A546A),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFF7F2FA),
)

/**
 * 应用级 Miuix 根主题。
 *
 * 把应用设置映射到 [ThemeController]：
 * - [theme]："system"/"light"/"dark" → System/Light/Dark（非取色模式）
 * - [colorSource]："monet" → Monet*（keyColor 为空，跟随系统壁纸色）；
 *   "cover" → Monet* + keyColor 取封面主色，由 Miuix 生成动态配色
 *
 * 状态栏/导航栏外观随明暗同步。
 */
@Composable
fun PureMusicTheme(
    theme: String,
    colorSource: String,
    coverAlbumId: Long?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // Miuix v0.9.4 的 ThemeController 属性均为 val，主题变更需重建控制器（与 Miuix 示例 AppTheme 一致）
    var keyColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(theme, colorSource, coverAlbumId) {
        keyColor = null
        if (colorSource == "cover" && coverAlbumId != null) {
            val loaded = loadCoverColors(context, coverAlbumId, brandFallbackColors, dark)
            // 取色成功（与回退不同）时以封面主色作为 Miuix 动态色种子；失败/无封面回退系统壁纸取色
            keyColor = if (loaded == brandFallbackColors) null else loaded.accent
        }
    }

    val isDynamicSource = colorSource == "monet" || colorSource == "cover"
    val colorSchemeMode = when {
        theme == "light" -> if (isDynamicSource) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        theme == "dark" -> if (isDynamicSource) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        else -> if (isDynamicSource) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
    }
    val controller = remember(theme, colorSource, colorSchemeMode, keyColor, dark) {
        ThemeController(colorSchemeMode = colorSchemeMode, keyColor = keyColor, isDark = dark)
    }

    // 同步状态栏和导航栏外观
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window ?: return
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !dark
        insetsController.isAppearanceLightNavigationBars = !dark
    }

    MiuixTheme(controller = controller) { content() }
}

