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

private enum class LibrarySection(val title: String) { SONGS("歌曲"), ALBUMS("专辑"), ARTISTS("艺术家"), FOLDERS("文件夹") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onPlaySong: (Song, List<Song>) -> Unit, onShowSettings: () -> Unit, viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory)) {
    val songs by viewModel.songs.collectAsStateWithLifecycle(); val albums by viewModel.albums.collectAsStateWithLifecycle(); val artists by viewModel.artists.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope(); var section by remember { mutableStateOf(LibrarySection.SONGS) }; var query by remember { mutableStateOf("") }; var searching by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current; val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE; var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; if (it) viewModel.refresh() }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { Text("PureMusic", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall); HorizontalDivider(); LibrarySection.entries.forEach { item -> NavigationDrawerItem(label = { Text(item.title) }, selected = section == item, onClick = { section = item; scope.launch { drawer.close() } }, icon = { Icon(if (item == LibrarySection.ALBUMS) Icons.Default.Album else if (item == LibrarySection.ARTISTS) Icons.Default.Person else if (item == LibrarySection.FOLDERS) Icons.Default.Folder else Icons.Default.MusicNote, null) }) }; HorizontalDivider(Modifier.padding(vertical = 12.dp)); NavigationDrawerItem(label = { Text("扫描音乐") }, selected = false, onClick = { viewModel.refresh(); scope.launch { drawer.close() } }, icon = { Icon(Icons.Default.Refresh, null) }); NavigationDrawerItem(label = { Text("设置") }, selected = false, onClick = { scope.launch { drawer.close() }; onShowSettings() }, icon = { Icon(Icons.Default.Settings, null) }) } }) {
        Scaffold(topBar = { TopAppBar(title = { if (searching) OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("搜索歌曲、专辑或艺术家") }, modifier = Modifier.fillMaxWidth()) else Text(section.title) }, navigationIcon = { IconButton(onClick = { if (searching) { searching = false; query = "" } else scope.launch { drawer.open() } }) { Icon(if (searching) Icons.Default.ArrowBack else Icons.Default.Menu, "导航") } }, actions = { IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Default.Search, "搜索") } }) }) { padding -> if (!granted) PermissionPanel(padding) { launcher.launch(permission) } else LibraryContent(section, songs, albums, artists, query, padding, onPlaySong) }
    }
}

@Composable private fun PermissionPanel(padding: PaddingValues, onGrant: () -> Unit) { Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); Text("允许访问本地音乐后开始扫描", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(16.dp)); Button(onClick = onGrant) { Text("授予权限") } } }

@Composable private fun LibraryContent(section: LibrarySection, songs: List<Song>, albums: List<Album>, artists: List<Artist>, query: String, padding: PaddingValues, onPlaySong: (Song, List<Song>) -> Unit) {
    val filtered = songs.filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    when (section) {
        LibrarySection.SONGS -> Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, null, tint = MaterialTheme.colorScheme.onSurface)
                Text("${filtered.size}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 28.dp).weight(1f))
                Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(22.dp)); Icon(Icons.Default.FormatListBulleted, null)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) { items(filtered, key = { it.id }) { SongRow(it) { onPlaySong(it, filtered) } } }
        }
        LibrarySection.ALBUMS -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(albums, key = { it.albumId }) { album -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { AlbumArt(album, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))); Spacer(Modifier.width(14.dp)); Column { Text(album.name, style = MaterialTheme.typography.titleMedium); Text("${album.artist} · ${album.songCount} 首", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        LibrarySection.ARTISTS -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp)) { items(artists, key = { it.name }) { artist -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Column { Text(artist.name, style = MaterialTheme.typography.titleMedium); Text("${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        LibrarySection.FOLDERS -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("文件夹视图将在扫描后提供", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun SongRow(song: Song, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { AlbumArt(song, Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium); Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(3.dp)) { Text("SQ", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }; Spacer(Modifier.width(7.dp)); Text("${song.artist} · ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) } }; Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(10.dp)); Icon(Icons.Default.MoreVert, null) } }

fun formatDuration(durationMs: Long): String { val seconds = durationMs / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }
