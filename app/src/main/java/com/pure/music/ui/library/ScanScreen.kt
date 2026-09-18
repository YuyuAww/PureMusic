package com.pure.music.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    viewModel: SettingsViewModel
) {
    val useMediaStore by viewModel.useMediaStore.collectAsStateWithLifecycle()
    val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()
    val skipShortTracks by viewModel.skipShortTracks.collectAsStateWithLifecycle()
    val blockedFolders by viewModel.blockedFolders.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeUri ->
            try {
                context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val path = resolveFolderPath(treeUri)
            if (path.isNotBlank()) {
                viewModel.addCustomFolder(path)
            }
        }
    }

    val addBlockedFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeUri ->
            val path = resolveFolderPath(treeUri)
            if (path.isNotBlank()) {
                viewModel.addBlockedFolder(path)
            }
        }
    }

    var showBlockedList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体来源") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. 媒体扫描操作
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    SettingRow("开始扫描", Icons.Default.Refresh, accent) { onScan() }
                }
            }

            // 2. 媒体来源配置
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("媒体来源", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
                    ToggleRow("使用 Android 媒体库", useMediaStore) { viewModel.setUseMediaStore(it) }
                    customFolders.forEach { path ->
                        CustomFolderRow(path) { viewModel.removeCustomFolder(path) }
                    }
                    SettingRow("添加自定义文件夹", Icons.Default.Add, accent) {
                        folderPicker.launch(null)
                    }
                }
            }

            // 3. 高级扫描设置
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("高级扫描设置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
                    SettingRow("管理外部存储权限", Icons.Default.OpenInNew, accent) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                    ToggleRow("不扫描 60 秒以下音频", skipShortTracks) { viewModel.setSkipShortTracks(it) }
                    SettingRow(
                        "被屏蔽的文件夹",
                        if (showBlockedList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        accent
                    ) {
                        showBlockedList = !showBlockedList
                    }
                    if (showBlockedList) {
                        if (blockedFolders.isNotEmpty()) {
                            blockedFolders.forEach { path ->
                                CustomFolderRow(path) { viewModel.setBlockedFolders(blockedFolders - path) }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("暂无被屏蔽的文件夹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        SettingRow("添加屏蔽文件夹", Icons.Default.Add, accent) {
                            addBlockedFolderPicker.launch(null)
                        }
                    }
                }
            }

            // 4. 扫描逻辑说明
            Text(
                "如果同时开启“使用 Android 媒体库”并添加了“自定义文件夹”，最终扫描到的歌曲是二者扫描结果的并集（合并）。如果只想使用自己指定的文件夹，请关闭“使用 Android 媒体库”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit, trailing: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(26.dp), tint = accent)
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) Icon(trailing, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomFolderRow(path: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Folder, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "移除", Modifier.size(18.dp)) }
    }
}

private fun resolveFolderPath(treeUri: Uri): String {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":")
        if (split.size < 2) return ""
        val type = split[0]
        val rawId = split[1]
        if (type == "primary") {
            "/storage/emulated/$rawId"
        } else {
            "/storage/${type}/${rawId}"
        }
    } catch (_: Exception) {
        ""
    }
}
