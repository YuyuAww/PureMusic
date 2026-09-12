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
    val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
    var showThemeDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showColorDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
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
            SettingRow("主题：${themeLabel(theme)}", Icons.Default.LightMode) { showThemeDialog = true }
            SettingRow("主题颜色：${colorSourceLabel(colorSource)}", Icons.Default.Palette) { showColorDialog = true }
            SettingRow("系统均衡器", Icons.Default.Equalizer)
            SettingRow("关于", Icons.Default.Info)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (showThemeDialog) ChoiceDialog("主题", listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色"), theme, { viewModel.setTheme(it); showThemeDialog = false }) { showThemeDialog = false }
    if (showColorDialog) ChoiceDialog("主题颜色", listOf("monet" to "Monet 取色", "cover" to "根据封面取色"), colorSource, { viewModel.setColorSource(it); showColorDialog = false }) { showColorDialog = false }
}

@Composable
private fun ChoiceDialog(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column { options.forEach { (value, label) -> ListItem(headlineContent = { Text(label) }, leadingContent = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) }, modifier = Modifier.clickable { onSelect(value) }) } }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}

@Composable private fun ProCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Surface(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } }
@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) { Surface(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, content = { Column(Modifier.padding(vertical = 8.dp), content = content) }) }
@Composable private fun SettingRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(20.dp)); Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null) } }
private fun themeLabel(theme: String) = when (theme) { "dark" -> "深色"; "light" -> "浅色"; else -> "跟随系统" }
private fun colorSourceLabel(source: String) = if (source == "cover") "根据封面取色" else "Monet 取色"
