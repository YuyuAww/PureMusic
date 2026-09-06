package com.pure.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.pure.music.data.Album
import com.pure.music.data.Song

@Composable
fun AlbumArt(albumId: Long, modifier: Modifier = Modifier, contentDescription: String? = null) {
    // MediaStore 的专辑封面 URI 只适用于本地媒体，不需要网络权限。
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SubcomposeAsyncImage(
            model = "content://media/external/audio/albumart/$albumId",
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
