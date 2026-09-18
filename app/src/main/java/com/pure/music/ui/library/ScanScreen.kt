package com.pure.music.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.data.MediaSource
import com.pure.music.settings.SettingsViewModel

private val MEDIA_SOURCES = listOf(
    MediaSource(id = "local", name = "本地文件", type = "local"),
    MediaSource(id = "cloud_qq", name = "QQ 音乐", type = "cloud"),
    MediaSource(id = "cloud_netease", name = "网易云音乐", type = "cloud")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    viewModel: SettingsViewModel
) {
    val enabled by viewModel.enabledMediaSources.collectAsStateWithLifecycle()
    val drawerAccent = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体来源") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).verticalScroll(rememberScrollState())) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("扫描来源", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
                    MEDIA_SOURCES.forEach { source ->
                        MediaSourceRow(source, source.id in enabled, drawerAccent) { viewModel.toggleMediaSource(source.id) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onScan, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("开始扫描")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MediaSourceRow(source: MediaSource, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (source.type == "cloud") Icons.Default.Cloud else Icons.Default.Folder,
            null,
            Modifier.size(28.dp),
            tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(20.dp))
        Text(source.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { onClick() })
    }
}
