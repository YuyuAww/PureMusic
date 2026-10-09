package com.pure.music.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.settings.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val themeOptions = listOf("跟随系统" to "system", "浅色" to "light", "深色" to "dark")
private val colorSourceOptions = listOf("Monet 取色" to "monet", "根据封面取色" to "cover")

/** 设置页：Miuix 偏好设置组件（SmallTitle + Card + OverlayDropdownPreference/ArrowPreference） */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            SmallTopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MiuixIcons.Back, "返回") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SmallTitle("外观", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                OverlayDropdownPreference(
                    items = themeOptions.map { it.first },
                    selectedIndex = themeOptions.indexOfFirst { it.second == theme }.coerceAtLeast(0),
                    title = "主题",
                    startAction = {
                        Icon(
                            MiuixIcons.Theme,
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onSurfaceVariantActions,
                        )
                    },
                    onSelectedIndexChange = { index ->
                        viewModel.setTheme(themeOptions[index].second)
                    },
                )
                HorizontalDivider()
                OverlayDropdownPreference(
                    items = colorSourceOptions.map { it.first },
                    selectedIndex = colorSourceOptions.indexOfFirst { it.second == colorSource }.coerceAtLeast(0),
                    title = "主题颜色",
                    startAction = {
                        Icon(
                            MiuixIcons.Theme,
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onSurfaceVariantActions,
                        )
                    },
                    onSelectedIndexChange = { index ->
                        viewModel.setColorSource(colorSourceOptions[index].second)
                    },
                )
            }
            SmallTitle("功能", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                ArrowPreference(
                    title = "DSP 均衡器",
                    summary = "调整各频段增益",
                    startAction = {
                        Icon(
                            MiuixIcons.Tune,
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onSurfaceVariantActions,
                        )
                    },
                )
                HorizontalDivider()
                ArrowPreference(
                    title = "音频输出",
                    summary = "蓝牙与独占模式",
                    startAction = {
                        Icon(
                            MiuixIcons.VolumeUp,
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onSurfaceVariantActions,
                        )
                    },
                )
            }

            SmallTitle("其他", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                ArrowPreference(
                    title = "关于",
                    summary = "版本与致谢",
                    startAction = {
                        Icon(
                            MiuixIcons.Info,
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onSurfaceVariantActions,
                        )
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
