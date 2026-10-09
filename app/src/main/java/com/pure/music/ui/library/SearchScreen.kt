package com.pure.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Song
import com.pure.music.library.LibraryViewModel
import com.pure.music.library.libraryViewModelFactory
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索页：Miuix SearchBar + 歌曲卡片结果列表 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        if (result) viewModel.refresh()
    }

    val colors = MiuixTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            SmallTopAppBar(
                title = "搜索",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MiuixIcons.Back, "返回") }
                },
            )
        },
    ) { padding ->
        if (!granted) {
            PermissionPanel(padding) { launcher.launch(Manifest.permission.READ_MEDIA_AUDIO) }
        } else {
            val results = songs
                .filter {
                    query.isBlank() ||
                        it.title.contains(query, true) ||
                        it.artist.contains(query, true) ||
                        it.album.contains(query, true)
                }
                .sortedBy { it.title.trim().lowercase() }
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                SearchBar(
                    inputField = {
                        InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = {},
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            label = "搜索歌曲、专辑或艺术家",
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    // 结果直接在下方列表展示，弹层内容留空
                }
                if (query.isNotBlank()) {
                    Text(
                        "${results.size} 首结果",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(results, key = { it.id }) { song ->
                        SongRow(song, song.id in favoriteIds, viewModel::toggleFavorite) {
                            onPlaySong(song, results)
                        }
                    }
                }
            }
        }
    }
}
