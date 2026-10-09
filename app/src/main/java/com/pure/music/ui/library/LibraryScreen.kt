package com.pure.music.ui.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberModalDrawerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pure.music.data.Album
import com.pure.music.data.Artist
import com.pure.music.data.MusicFolder
import com.pure.music.data.Song
import com.pure.music.library.LibraryViewModel
import com.pure.music.library.libraryViewModelFactory
import com.pure.music.ui.components.AlbumArt
import com.pure.music.ui.components.MiuixDrawer
import com.pure.music.ui.components.MiuixDrawerItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Sidebar
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class LibrarySection(val title: String) {
    SONGS("歌曲"), ALBUMS("专辑"), ARTISTS("艺术家"), FOLDERS("文件夹"), FAVORITES("收藏"), RECENT("最近播放")
}

/** 媒体库页：Miuix 风格侧边抽屉（应用自有封装 MiuixDrawer）+ Miuix 顶栏与卡片列表 */
@Composable
fun LibraryScreen(
    onPlaySong: (Song, List<Song>) -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onShowScan: () -> Unit,
    onExit: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val recentIds by viewModel.recentSongIds.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(LibrarySection.SONGS) }
    var folderPath by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberModalDrawerState()
    val scope = rememberCoroutineScope()
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

    MiuixDrawer(
        drawerState = drawerState,
        items = buildList {
            LibrarySection.entries.forEach { s ->
                add(
                    MiuixDrawerItem(
                        icon = sectionIcon(s),
                        title = s.title,
                        selected = section == s && folderPath == null,
                        onClick = {
                            section = s
                            folderPath = null
                            scope.launch { drawerState.close() }
                        },
                    )
                )
            }
            add(
                MiuixDrawerItem(MiuixIcons.Refresh, "扫描媒体", false) {
                    scope.launch { drawerState.close() }
                    onShowScan()
                }
            )
            add(
                MiuixDrawerItem(MiuixIcons.Settings, "设置", false) {
                    scope.launch { drawerState.close() }
                    onShowSettings()
                }
            )
            add(
                MiuixDrawerItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "退出",
                    selected = false,
                    onClick = onExit,
                )
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MiuixLibraryContent(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onShowSearch = onShowSearch,
                onShowScan = onShowScan,
                granted = granted,
                onRequestPermission = { launcher.launch(Manifest.permission.READ_MEDIA_AUDIO) },
                songs = songs,
                albums = albums,
                artists = artists,
                folders = folders,
                favoriteIds = favoriteIds,
                recentIds = recentIds,
                section = section,
                folderPath = folderPath,
                onOpenFolder = { folderPath = it },
                onCloseFolder = { folderPath = null },
                onPlaySong = onPlaySong,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }
}

private fun sectionIcon(section: LibrarySection): ImageVector = when (section) {
    LibrarySection.SONGS -> MiuixIcons.Music
    LibrarySection.ALBUMS -> MiuixIcons.Album
    LibrarySection.ARTISTS -> MiuixIcons.Contacts
    LibrarySection.FOLDERS -> MiuixIcons.Folder
    LibrarySection.FAVORITES -> MiuixIcons.Favorites
    LibrarySection.RECENT -> MiuixIcons.Recent
}


@Composable
private fun MiuixLibraryContent(
    onOpenDrawer: () -> Unit,
    onShowSearch: () -> Unit,
    onShowScan: () -> Unit,
    granted: Boolean,
    onRequestPermission: () -> Unit,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    folders: List<MusicFolder>,
    favoriteIds: Set<Long>,
    recentIds: List<Long>,
    section: LibrarySection,
    folderPath: String?,
    onOpenFolder: (String) -> Unit,
    onCloseFolder: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val title = when {
        folderPath != null -> "文件夹"
        else -> section.title
    }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { if (folderPath != null) onCloseFolder() else onOpenDrawer() }) {
                        Icon(
                            imageVector = if (folderPath != null) MiuixIcons.Back else MiuixIcons.Basic.Sidebar,
                            contentDescription = if (folderPath != null) "返回" else "菜单",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShowSearch) { Icon(MiuixIcons.Search, "搜索") }
                    IconButton(onClick = onShowScan) { Icon(MiuixIcons.Refresh, "扫描") }
                },
            )
        },
    ) { padding ->
        if (!granted) {
            PermissionPanel(padding, onRequestPermission)
        } else when {
            folderPath != null -> {
                val folderSongs = songs.filter { it.path.substringBeforeLast('/', "") == folderPath }
                SongList(folderSongs, favoriteIds, onToggleFavorite, onPlaySong, padding)
            }
            else -> when (section) {
                LibrarySection.SONGS -> SongSection(songs, favoriteIds, onToggleFavorite, onPlaySong, padding)
                LibrarySection.ALBUMS -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(albums, key = { it.albumId }) { album -> AlbumCard(album) }
                }
                LibrarySection.ARTISTS -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    items(artists, key = { it.name }) { artist -> ArtistCard(artist) }
                }
                LibrarySection.FOLDERS -> LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    items(folders, key = { it.path }) { folder ->
                        Card(
                            onClick = { onOpenFolder(folder.path) },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(MiuixIcons.Folder, null, Modifier.size(34.dp), tint = colors.primary)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(folder.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = folder.path,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 13.sp,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                }
                                Text("${folder.songCount} 首", fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
                            }
                        }
                    }
                }
                LibrarySection.FAVORITES -> SongList(
                    songs.filter { it.id in favoriteIds },
                    favoriteIds, onToggleFavorite, onPlaySong, padding
                )
                LibrarySection.RECENT -> SongList(
                    recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } },
                    favoriteIds, onToggleFavorite, onPlaySong, padding
                )
            }
        }
    }
}


@Composable
private fun SongSection(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    padding: PaddingValues,
) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    val filtered = songs.filter { it.durationMs > 30_000 }
    Box(Modifier.fillMaxSize()) {
        SongList(filtered, favoriteIds, onToggleFavorite, onPlaySong, padding, listState)
        if (filtered.size > 1) AlphabetIndex(filtered, listState, Modifier.align(Alignment.CenterEnd))
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .background(color = colors.background),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(20.dp), tint = colors.onSurface)
                Spacer(Modifier.width(24.dp))
                Text(
                    "${filtered.size} 首",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.SortByAlpha, null, Modifier.size(20.dp), tint = colors.onSurface)
                Spacer(Modifier.width(18.dp))
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, Modifier.size(20.dp), tint = colors.onSurface)
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    padding: PaddingValues,
    listState: LazyListState? = null,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(vertical = 8.dp),
        state = listState ?: rememberLazyListState(),
    ) {
        items(songs, key = { it.id }) { song ->
            SongRow(song, song.id in favoriteIds, onToggleFavorite) { onPlaySong(song, songs) }
        }
    }
}

/** Miuix 卡片风格的歌曲行：点击整卡播放，右侧可收藏 */
@Composable
fun SongRow(
    song: Song,
    isFavorite: Boolean = false,
    onToggleFavorite: (Long) -> Unit = {},
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        BasicComponent(
            title = song.title,
            summary = "${song.artist} · ${song.album}",
            startAction = {
                AlbumArt(song, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
            },
            endActions = {
                IconButton(onClick = { onToggleFavorite(song.id) }) {
                    Icon(
                        if (isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        if (isFavorite) "取消收藏" else "收藏",
                        tint = colors.primary,
                    )
                }
            },
        )
    }
}


@Composable
private fun AlbumCard(album: Album) {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        insideMargin = PaddingValues(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(album, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(album.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${album.artist} · ${album.songCount} 首",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(artist: Artist) {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        insideMargin = PaddingValues(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(MiuixIcons.Contacts, null, Modifier.size(32.dp), tint = colors.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(artist.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 权限申请占位页（minSdk 33 仅需 READ_MEDIA_AUDIO） */
@Composable
fun PermissionPanel(padding: PaddingValues, onGrant: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = colors.primary)
        Spacer(Modifier.height(16.dp))
        Text("允许访问本地音乐后开始扫描", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrant) {
            Text("授予权限", fontSize = 15.sp)
        }
    }
}

@Composable
private fun AlphabetIndex(
    songs: List<Song>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    Column(
        modifier
            .padding(end = 6.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                fontSize = 10.sp,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
