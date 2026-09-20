package com.pure.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.pure.music.data.Album
import com.pure.music.data.Song
import com.pure.music.library.MediaLibraryRepository

@Composable
fun AlbumArt(albumId: Long, modifier: Modifier = Modifier, contentDescription: String? = null) {
    // 优先使用 TagLib 写入缓存目录的内嵌封面文件；缺失时回退 MediaStore 专辑封面 URI
    val context = LocalContext.current
    val repository = remember(context) { MediaLibraryRepository.get(context) }
    val generation = repository.coversGeneration.collectAsStateWithLifecycle().value
    val model: Any = remember(generation, albumId) {
        repository.getEmbeddedCoverFile(albumId) ?: "content://media/external/audio/albumart/$albumId"
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = { FallbackIcon() },
            loading = { FallbackIcon() }
        )
    }
}

@Composable
private fun FallbackIcon() {
    Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
}

@Composable
fun AlbumArt(song: Song, modifier: Modifier = Modifier) = AlbumArt(song.albumId, modifier, song.album)

@Composable
fun AlbumArt(album: Album, modifier: Modifier = Modifier) = AlbumArt(album.albumId, modifier, album.name)
