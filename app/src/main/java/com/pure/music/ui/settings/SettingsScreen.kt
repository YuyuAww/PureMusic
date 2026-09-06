package com.pure.music.ui.settings

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.settings.SettingsViewModel

/**
 * 设置界面。
 * 包含外观主题切换、系统均衡器外链、关于信息和开源许可。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 外观设置：主题切换
        SectionTitle("外观")
        HorizontalDivider()

        ThemeOption("跟随系统", "system", theme, { viewModel.setTheme("system") })
        ThemeOption("亮色", "light", theme, { viewModel.setTheme("light") })
        ThemeOption("暗色", "dark", theme, { viewModel.setTheme("dark") })

        HorizontalDivider()

        // 系统均衡器外链（Android 10+ 原生 EQ API 已弃用，采用深链方案）
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_SOUND_SETTINGS)
                        )
                    } catch (_: Exception) {
                        // 设备不支持系统均衡器时忽略
                    }
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "系统均衡器",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        // 关于信息
        Spacer(Modifier.height(16.dp))
        SectionTitle("关于")

        // 获取应用版本号
        val version = try {
            val info: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )
            "${info.versionName} (v${info.versionCode})"
        } catch (e: Exception) {
            "未知"
        }

        InfoRow("版本", version)
        InfoRow("ABI", Build.SUPPORTED_ABIS.joinToString())
        InfoRow("Android", Build.VERSION.RELEASE)

        Spacer(Modifier.height(24.dp))

        // 开源许可
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // 可后续增加许可对话框
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "开源许可",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "PureMusic 是一个本地音乐播放器，使用 Media3 ExoPlayer、Jetpack Compose 和 Material Design 构建。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/** 设置分区标题 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(16.dp)
    )
}

/** 主题选项行，使用 RadioButton 选择 */
@Composable
private fun ThemeOption(
    label: String,
    value: String,
    currentValue: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentValue == value,
            onClick = { onSelect() }
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/** 信息行，显示标签和值（如版本号、ABI 等） */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
