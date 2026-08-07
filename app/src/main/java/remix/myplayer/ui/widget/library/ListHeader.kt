package remix.myplayer.ui.widget.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.Command
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ext.clickableWithoutRipple

@Composable
fun SongListHeader(songs: List<Song>) {
  if (songs.isEmpty()) {
    return
  }
  Row(
    modifier = Modifier
      .height(48.dp)
      .fillMaxWidth()
      .background(LocalTheme.current.mainBackground)
      .clickableWithoutRipple(remember { MutableInteractionSource() }) {
        if (songs.isEmpty()) {
          MessageNotifier.show(R.string.no_song)
          return@clickableWithoutRipple
        }
        setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.SKIP_TO_NEXT, true))
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.padding(start = 16.dp, end = 8.dp),
      painter = painterResource(R.drawable.ic_shuffle_white_24dp),
      tint = LocalTheme.current.secondary,
      contentDescription = "ListHeaderIcon"
    )
    Text(
      text = stringResource(R.string.play_random, songs.size),
      color = LocalTheme.current.textSecondary
    )
  }
}