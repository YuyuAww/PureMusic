package com.pure.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.settings.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 24.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("设置", style = MaterialTheme.typography.headlineMedium)
        }
        ProCard("Salt Player Pro", "包含 Salt Player 均衡器及 10+ 项高级功能", Icons.Default.WorkspacePremium)
        ProCard("Audiophile Pack", "USB 独占输出、DAC 控制与 Morvanium", Icons.Default.GraphicEq)
        SettingsCard {
            SettingRow("Morvanium", Icons.Default.AutoAwesome)
        }
        SettingsCard {
            SettingRow("用户界面", Icons.Default.Palette)
            SettingRow("无障碍", Icons.Default.Accessibility)
            SettingRow("歌词", Icons.Default.FormatQuote)
            SettingRow("车载", Icons.Default.DirectionsCar)
            SettingRow("音频输出", Icons.Default.VolumeUp)
            SettingRow("USB 独占模式", Icons.Default.Usb)
            SettingRow("通知", Icons.Default.Notifications)
            SettingRow("启动与后台", Icons.Default.RocketLaunch)
        }
        SettingsCard {
            SettingRow("主题：${themeLabel(theme)}", Icons.Default.LightMode) { viewModel.setTheme(if (theme == "dark") "light" else "dark") }
            SettingRow("系统均衡器", Icons.Default.Equalizer)
            SettingRow("关于", Icons.Default.Info)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun ProCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Surface(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } }
@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) { Surface(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, content = { Column(Modifier.padding(vertical = 8.dp), content = content) }) }
@Composable private fun SettingRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(20.dp)); Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null) } }
private fun themeLabel(theme: String) = when (theme) { "dark" -> "深色"; "light" -> "浅色"; else -> "跟随系统" }
