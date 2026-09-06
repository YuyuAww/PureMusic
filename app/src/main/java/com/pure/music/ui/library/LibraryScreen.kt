package com.pure.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Album
import com.pure.music.data.Artist
import com.pure.music.data.Song
import com.pure.music.data.db.PlaylistEntity
import com.pure.music.library.FavoritesViewModel
import com.pure.music.library.LibraryViewModel
import com.pure.music.library.PlaylistViewModel
import com.pure.music.library.favoritesViewModelFactory
import com.pure.music.library.libraryViewModelFactory
import com.pure.music.library.playlistViewModelFactory
import com.pure.music.ui.album.AlbumDetailScreen
import com.pure.music.ui.playlist.PlaylistDetailScreen

/** 媒体库标签页枚举 */
private enum class LibraryTab(val label: String) {
    SONGS("歌曲"),
    ALBUMS("专辑"),
    ARTISTS("艺术家"),
    FAVORITES("收藏"),
    PLAYLISTS("歌单")
}

/** 歌曲排序方式枚举 */
private enum class SortOrder(val label: String) {
    TITLE("按标题"),
    ARTIST("按艺术家"),
    ALBUM("按专辑"),
    DATE_ADDED("按添加日期"),
    DURATION("按时长")
}

/**
 * 媒体库主界面。
 * 包含权限申请、搜索、排序、标签页切换，以及歌曲/专辑/艺术家/收藏/歌单五个列表视图。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onShowSettings: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory)
) {
    val context = LocalContext.current
    // 根据 Android 版本选择正确的存储权限
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体库") },
                actions = {
                    IconButton(onClick = onShowSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        if (!permissionGranted) {
            PermissionRequestCard(
                message = "需要音频读取权限以扫描您的本地音乐",
                onGrant = { launcher.launch(permission) }
            )
        } else {
            LibraryContent(
                viewModel = viewModel,
                onPlaySong = onPlaySong,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/** 权限申请卡片，首次启动时显示 */
@Composable
private fun PermissionRequestCard(
    message: String,
    onGrant: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onGrant) {
            Text("授权并扫描")
        }
    }
}

/**
 * 媒体库内容区，包含搜索、排序、标签页和对话框管理。
 * 是媒体库界面的核心组件，协调所有子视图和弹窗。
 */
@Composable
private fun LibraryContent(
    viewModel: LibraryViewModel,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }

    val favoritesViewModel: FavoritesViewModel = viewModel(factory = favoritesViewModelFactory)
    val playlistViewModel: PlaylistViewModel = viewModel(factory = playlistViewModelFactory)
    val favoriteIds by favoritesViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()

    // 对话框和详情界面状态
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    // 搜索和排序状态
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 按搜索关键词过滤歌曲（标题/艺术家/专辑）
    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 按排序方式对歌曲排序
    val sortedSongs = remember(filteredSongs, sortOrder) {
        when (sortOrder) {
            SortOrder.TITLE -> filteredSongs.sortedBy { it.title }
            SortOrder.ARTIST -> filteredSongs.sortedBy { it.artist }
            SortOrder.ALBUM -> filteredSongs.sortedBy { it.album }
            SortOrder.DATE_ADDED -> filteredSongs.sortedByDescending { it.dateAdded }
            SortOrder.DURATION -> filteredSongs.sortedBy { it.duration }
        }
    }

    // 按搜索关键词过滤专辑
    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums else {
            albums.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 按搜索关键词过滤艺术家
    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isBlank()) artists else {
            artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 搜索栏 + 排序菜单
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                placeholder = { Text("搜索歌曲、艺术家、专辑") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "排序")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                sortOrder = order
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 标签页切换
        TabRow(
            selectedTabIndex = selectedTab.ordinal
        ) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) }
                )
            }
        }

        // 根据选中标签显示对应内容
        when (selectedTab) {
            LibraryTab.SONGS -> SongsList(
                songs = sortedSongs,
                onPlay = { song -> onPlaySong(song, sortedSongs) },
                onShowMenu = { song -> selectedSong = song },
                modifier = Modifier.fillMaxSize()
            )
            LibraryTab.ALBUMS -> AlbumsGrid(
                albums = filteredAlbums,
                songs = songs,
                onAlbumClick = { album -> selectedAlbum = album },
                modifier = Modifier.fillMaxSize()
            )
            LibraryTab.ARTISTS -> ArtistsList(filteredArtists, Modifier.fillMaxSize())
            LibraryTab.FAVORITES -> FavoritesList(
                songs = sortedSongs.filter { favoriteIds.contains(it.id) },
                onPlay = { song, queue -> onPlaySong(song, queue) },
                onShowMenu = { song -> selectedSong = song },
                modifier = Modifier.fillMaxSize()
            )
            LibraryTab.PLAYLISTS -> PlaylistsList(
                playlists = playlists,
                songs = songs,
                onPlaylistClick = { playlist -> selectedPlaylist = playlist },
                onCreatePlaylist = { showCreatePlaylistDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 歌曲上下文菜单
    selectedSong?.let { song ->
        SongMenuDialog(
            song = song,
            isFavorite = favoriteIds.contains(song.id),
            playlists = playlists,
            onToggleFavorite = { favoritesViewModel.toggleFavorite(song.id) },
            onAddToPlaylist = { playlistId ->
                playlistViewModel.addSongToPlaylist(playlistId, song.id)
            },
            onDismiss = { selectedSong = null }
        )
    }

    // 专辑详情
    selectedAlbum?.let { album ->
        val albumSongs = songs.filter { it.albumId == album.albumId }
            .sortedBy { it.trackNumber }
        AlbumDetailScreen(
            album = album,
            songs = albumSongs,
            onBack = { selectedAlbum = null },
            onPlayAll = { onPlaySong(albumSongs.first(), albumSongs) },
            onPlaySong = { song, queue -> onPlaySong(song, queue) }
        )
    }

    // 歌单详情
    selectedPlaylist?.let { playlist ->
        val playlistSongs = songs.filter { playlist.songIds.contains(it.id) }
        PlaylistDetailScreen(
            playlistName = playlist.name,
            songs = playlistSongs,
            onBack = { selectedPlaylist = null },
            onPlayAll = { if (playlistSongs.isNotEmpty()) onPlaySong(playlistSongs.first(), playlistSongs) },
            onPlaySong = { song, queue -> onPlaySong(song, queue) },
            onRemoveSong = { songId ->
                playlistViewModel.removeSongFromPlaylist(playlist.id, songId)
            }
        )
    }

    // 新建歌单对话框
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                playlistViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
}

/** 歌曲列表视图 */
@Composable
private fun SongsList(
    songs: List<Song>,
    onPlay: (Song) -> Unit = {},
    onShowMenu: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        EmptyStateView("暂无歌曲", modifier)
        return
    }
    LazyColumn(modifier = modifier) {
        items(songs, key = { it.id }) { song ->
            SongListItem(song, onPlay, onShowMenu)
        }
    }
}

/** 歌曲列表项，包含封面、标题、时长、播放按钮和更多菜单 */
@Composable
private fun SongListItem(
    song: Song,
    onPlay: (Song) -> Unit = {},
    onShowMenu: (Song) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(song) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { onPlay(song) }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "播放")
        }
        IconButton(onClick = { onShowMenu(song) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多")
        }
    }
}

/** 专辑网格视图，3 列布局 */
@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    songs: List<Song>,
    onAlbumClick: (Album) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        EmptyStateView("暂无专辑", modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums.chunked(3)) { rowAlbums ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowAlbums.forEach { album ->
                    AlbumGridItem(
                        album,
                        onAlbumClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 专辑网格项，正方形封面占位 + 专辑名和艺术家 */
@Composable
private fun AlbumGridItem(
    album: Album,
    onClick: (Album) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick(album) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = album.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = album.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 艺术家列表视图 */
@Composable
private fun ArtistsList(artists: List<Artist>, modifier: Modifier = Modifier) {
    if (artists.isEmpty()) {
        EmptyStateView("暂无艺术家", modifier)
        return
    }
    LazyColumn(modifier = modifier) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
        )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.albumCount} 张专辑 · ${artist.songCount} 首",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 收藏歌曲列表视图 */
@Composable
private fun FavoritesList(
    songs: List<Song>,
    onPlay: (Song, List<Song>) -> Unit = { _, _ -> },
    onShowMenu: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        EmptyStateView("暂无收藏", modifier)
        return
    }
    LazyColumn(modifier = modifier) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(song, songs) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} · ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onPlay(song, songs) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                }
                IconButton(onClick = { onShowMenu(song) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }
        }
    }
}

/** 歌单列表视图，包含新建歌单入口和歌单条目 */
@Composable
private fun PlaylistsList(
    playlists: List<PlaylistEntity>,
    songs: List<Song>,
    onPlaylistClick: (PlaylistEntity) -> Unit = {},
    onCreatePlaylist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreatePlaylist() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "新建歌单",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Text(
                    text = "暂无歌单，点击上方新建",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            val songCount = songs.count { playlist.songIds.contains(it.id) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${songCount} 首歌曲",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onPlaylistClick(playlist) }) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = "播放")
                }
            }
        }
    }
}

/** 新建歌单对话框 */
@Composable
private fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建歌单") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("歌单名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/** 空状态占位视图 */
@Composable
private fun EmptyStateView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 时长格式化：毫秒 → "分:秒" */
internal fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
