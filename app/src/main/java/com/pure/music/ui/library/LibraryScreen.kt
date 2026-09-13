package com.pure.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.*
import com.pure.music.library.*
import com.pure.music.ui.components.AlbumArt
import kotlinx.coroutines.launch

private enum class LibrarySection(val title: String) { SONGS("歌曲"), ALBUMS("专辑"), ARTISTS("艺术家"), FOLDERS("文件夹"), FAVORITES("收藏"), RECENT("最近播放") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlaySong: (Song, List<Song>) -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onExit: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onShowEqualizer: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory)
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle(); val albums by viewModel.albums.collectAsStateWithLifecycle(); val artists by viewModel.artists.collectAsStateWithLifecycle(); val folders by viewModel.folders.collectAsStateWithLifecycle(); val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle(); val recentIds by viewModel.recentSongIds.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope(); var section by remember { mutableStateOf(LibrarySection.SONGS) }; var folderPath by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current; val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE; var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; if (it) viewModel.refresh() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactDrawerWidth = (maxWidth * 0.5f).coerceIn(280.dp, 360.dp)
        val drawerItems: @Composable ColumnScope.() -> Unit = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExit) { Icon(Icons.Default.ExitToApp, "退出应用") }
                        IconButton(onClick = onToggleTheme) { Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, if (isDarkTheme) "切换浅色模式" else "切换深色模式") }
                        IconButton(onClick = { scope.launch { drawer.close() }; onShowEqualizer() }) { Icon(Icons.Default.Equalizer, "均衡器") }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        LibrarySection.entries.forEach { item -> NavigationDrawerItem(label = { Text(item.title) }, selected = section == item, onClick = { section = item; folderPath = null; scope.launch { drawer.close() } }, icon = { Icon(if (item == LibrarySection.ALBUMS) Icons.Default.Album else if (item == LibrarySection.ARTISTS) Icons.Default.Person else if (item == LibrarySection.FOLDERS) Icons.Default.Folder else if (item == LibrarySection.FAVORITES) Icons.Default.Favorite else if (item == LibrarySection.RECENT) Icons.Default.History else Icons.Default.MusicNote, null) }) }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        NavigationDrawerItem(label = { Text("扫描音乐") }, selected = false, onClick = { viewModel.refresh(); scope.launch { drawer.close() } }, icon = { Icon(Icons.Default.Refresh, null) })
                        NavigationDrawerItem(label = { Text("设置") }, selected = false, onClick = { scope.launch { drawer.close() }; onShowSettings() }, icon = { Icon(Icons.Default.Settings, null) })
                    }
                }
            }
        }
        val content: @Composable () -> Unit = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (folderPath == null) section.title else folders.firstOrNull { it.path == folderPath }?.name ?: "文件夹") },
                        navigationIcon = {
                            if (folderPath != null) IconButton(onClick = { folderPath = null }) { Icon(Icons.Default.ArrowBack, "返回") }
                            else if (maxWidth < 600.dp) {
                                IconButton(onClick = { scope.launch { if (drawer.isOpen) drawer.close() else drawer.open() } }) {
                                    Icon(Icons.Default.Menu, "导航")
                                }
                            }
                        },
                        actions = { IconButton(onClick = onShowSearch) { Icon(Icons.Default.Search, "搜索") } }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().imePadding()) {
                    if (!granted) PermissionPanel(padding) { launcher.launch(permission) }
                    else LibraryContent(section, songs, albums, artists, folders, favoriteIds, recentIds, folderPath, "", padding, onPlaySong, viewModel::toggleFavorite) { folderPath = it }
                }
            }
        }
        if (maxWidth >= 600.dp) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(compactDrawerWidth),
                        content = drawerItems
                    )
                },
                content = content
            )
        } else {
            DismissibleNavigationDrawer(
                drawerState = drawer,
                drawerContent = { DismissibleDrawerSheet(modifier = Modifier.width(compactDrawerWidth), content = drawerItems) },
                content = content
            )
        }
    }
}

@Composable fun PermissionPanel(padding: PaddingValues, onGrant: () -> Unit) { Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); Text("允许访问本地音乐后开始扫描", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(16.dp)); Button(onClick = onGrant) { Text("授予权限") } } }

@Composable private fun LibraryContent(section: LibrarySection, songs: List<Song>, albums: List<Album>, artists: List<Artist>, folders: List<MusicFolder>, favoriteIds: Set<Long>, recentIds: List<Long>, folderPath: String?, query: String, padding: PaddingValues, onPlaySong: (Song, List<Song>) -> Unit, onToggleFavorite: (Long) -> Unit, onOpenFolder: (String) -> Unit) {
    val filtered = songs
        .filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
        .sortedBy { it.title.trim().lowercase() }
    when (section) {
        LibrarySection.SONGS -> Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, null, tint = MaterialTheme.colorScheme.onSurface)
                Text("${filtered.size}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 28.dp).weight(1f))
                Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(22.dp)); Icon(Icons.Default.FormatListBulleted, null)
            }
            Box(Modifier.weight(1f)) {
                val listState = rememberLazyListState()
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(filtered, key = { it.id }) { SongRow(it, it.id in favoriteIds, onToggleFavorite) { onPlaySong(it, filtered) } }
                }
                AlphabetIndex(filtered, listState, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
            }
        }
        LibrarySection.ALBUMS -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(albums, key = { it.albumId }) { album -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { AlbumArt(album, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))); Spacer(Modifier.width(14.dp)); Column { Text(album.name, style = MaterialTheme.typography.titleMedium); Text("${album.artist} · ${album.songCount} 首", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        LibrarySection.ARTISTS -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp)) { items(artists, key = { it.name }) { artist -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Column { Text(artist.name, style = MaterialTheme.typography.titleMedium); Text("${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        LibrarySection.FAVORITES -> SongList(songs.filter { it.id in favoriteIds }, favoriteIds, onToggleFavorite, onPlaySong, padding)
        LibrarySection.RECENT -> {
            val recentSongs = recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
            SongList(recentSongs, favoriteIds, onToggleFavorite, onPlaySong, padding)
        }
        LibrarySection.FOLDERS -> if (folderPath == null) {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(folders, key = { it.path }) { folder -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenFolder(folder.path) }.background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Folder, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(folder.name, style = MaterialTheme.typography.titleMedium); Text(folder.path, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${folder.songCount} 首") } } }
        } else {
            val folderSongs = songs.filter { it.path.substringBeforeLast('/', "") == folderPath }
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) { items(folderSongs, key = { it.id }) { SongRow(it, it.id in favoriteIds, onToggleFavorite) { onPlaySong(it, folderSongs) } } }
        }
    }
}

@Composable private fun SongList(songs: List<Song>, favoriteIds: Set<Long>, onToggleFavorite: (Long) -> Unit, onPlaySong: (Song, List<Song>) -> Unit, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(songs, key = { it.id }) { song -> SongRow(song, song.id in favoriteIds, onToggleFavorite) { onPlaySong(song, songs) } }
    }
}

@Composable fun SongRow(song: Song, isFavorite: Boolean = false, onToggleFavorite: (Long) -> Unit = {}, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AlbumArt(song, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(3.dp)) { Text("SQ", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.width(7.dp)); Text("${song.artist} · ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        IconButton(onClick = { onToggleFavorite(song.id) }) { Icon(Icons.Default.PlaylistAdd, "添加到歌单", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Default.MoreVert, "更多操作")
    }
}

@Composable
private fun AlphabetIndex(songs: List<Song>, listState: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ('A'..'Z').forEach { letter ->
            Text(
                letter.toString(),
                modifier = Modifier.clickable {
                    val index = songs.indexOfFirst { song ->
                        val first = song.title.trim().firstOrNull()?.uppercaseChar()
                        first == letter
                    }
                    if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatDuration(durationMs: Long): String { val seconds = durationMs / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }
