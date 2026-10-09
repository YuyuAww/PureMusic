package com.pure.music.ui.library

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pure.music.settings.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 媒体来源页：Miuix 偏好设置风格（SmallTitle + Card + ArrowPreference/SwitchPreference） */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val skipShortTracks by viewModel.skipShortTracks.collectAsStateWithLifecycle()
    val blockedFolders by viewModel.blockedFolders.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme
    var showBlockedList by remember { mutableStateOf(false) }

    val addBlockedFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeUri ->
            val path = resolveFolderPath(treeUri)
            if (path.isNotBlank()) viewModel.addBlockedFolder(path)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            SmallTopAppBar(
                title = "媒体来源",
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
                .verticalScroll(rememberScrollState())
        ) {
            SmallTitle("媒体扫描", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                ArrowPreference(
                    title = "开始扫描",
                    summary = "重新扫描 MediaStore 并用 TagLib 刷新标签缓存",
                    startAction = { Icon(MiuixIcons.Refresh, null, Modifier.size(24.dp)) },
                    onClick = onScan,
                )
            }
            SmallTitle("高级扫描设置", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                SwitchPreference(
                    title = "跳过短音频",
                    summary = "不扫描时长小于 30 秒的音频片段",
                    checked = skipShortTracks,
                    onCheckedChange = viewModel::setSkipShortTracks,
                )
                HorizontalDivider()
                ArrowPreference(
                    title = "屏蔽文件夹列表",
                    summary = if (blockedFolders.isEmpty()) "暂无屏蔽文件夹" else "${blockedFolders.size} 个文件夹",
                    startAction = { Icon(MiuixIcons.Folder, null, Modifier.size(24.dp)) },
                    endActions = {
                        IconButton(onClick = { showBlockedList = !showBlockedList }) {
                            Icon(
                                if (showBlockedList) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                                if (showBlockedList) "收起" else "展开",
                            )
                        }
                    },
                )
                if (showBlockedList) {
                    HorizontalDivider()
                    if (blockedFolders.isEmpty()) {
                        Text(
                            "暂无被屏蔽的文件夹",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    } else {
                        blockedFolders.forEach { path ->
                            BlockedFolderRow(path) { viewModel.setBlockedFolders(blockedFolders - path) }
                        }
                    }
                    HorizontalDivider()
                    ArrowPreference(
                        title = "添加屏蔽文件夹",
                        startAction = { Icon(MiuixIcons.Add, null, Modifier.size(24.dp)) },
                        onClick = { addBlockedFolderPicker.launch(null) },
                    )
                }
            }

            Text(
                "歌曲始终从 Android 媒体库扫描，并使用 TagLib 读取完整元数据；TagLib 读取失败时回退使用媒体库提供的基本信息。",
                fontSize = 13.sp,
                color = colors.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BlockedFolderRow(path: String, onRemove: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MiuixIcons.Folder, null, Modifier.size(20.dp), tint = colors.onSurfaceVariantSummary)
        Spacer(Modifier.width(14.dp))
        Text(
            path,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) { Icon(MiuixIcons.Close, "移除", Modifier.size(18.dp)) }
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
