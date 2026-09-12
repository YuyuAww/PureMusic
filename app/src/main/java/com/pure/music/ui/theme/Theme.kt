package com.pure.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pure.music.ui.utils.CoverColors
import com.pure.music.ui.utils.loadCoverColors

/** 暗色主题配色方案 */
private val DarkColorScheme = darkColorScheme(
    primary = BrandDarkPrimary,
    secondary = BrandAccent,
    tertiary = BrandDarkPrimary,
    surface = BrandDarkSurface,
    onSurface = BrandDarkOnSurface,
    background = BrandDarkSurface,
    surfaceVariant = BrandDarkSurfaceVariant
)

/** 亮色主题配色方案 */
private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandAccent,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    background = Color(0xFFFFF9FD),
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant
)

/**
 * PureMusic 应用主题 Composable。
 * 支持动态取色（Android 12+）、明暗模式切换、状态栏/导航栏颜色同步。
 */
@Composable
fun PureMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorSource: String = "monet",
    coverAlbumId: Long? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val fallback = CoverColors(BrandPrimary, BrandOnSurfaceVariant, BrandSurface, BrandSurface)
    var coverColors by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(fallback) }
    androidx.compose.runtime.LaunchedEffect(colorSource, coverAlbumId) {
        coverColors = if (colorSource == "cover" && coverAlbumId != null) loadCoverColors(context, coverAlbumId, fallback) else fallback
    }
    val colorScheme = when {
        // Android 12+ 使用系统动态取色
        dynamicColor && colorSource == "monet" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        colorSource == "cover" -> if (darkTheme) darkColorScheme(
            primary = coverColors.accent,
            secondary = coverColors.muted,
            background = coverColors.background,
            surface = coverColors.surface,
            onSurface = Color.White
        ) else lightColorScheme(
            primary = coverColors.accent,
            secondary = coverColors.muted,
            background = coverColors.background,
            surface = coverColors.surface,
            onSurface = Color.Black
        )
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 同步状态栏和导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.surface.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
