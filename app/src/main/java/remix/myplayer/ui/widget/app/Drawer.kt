package remix.myplayer.ui.widget.app

import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.data.model.misc.Library
import remix.myplayer.data.prefs.ThemePrefs.Companion.BLACK
import remix.myplayer.data.prefs.ThemePrefs.Companion.DARK
import remix.myplayer.data.prefs.ThemePrefs.Companion.LIGHT
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteHistory
import remix.myplayer.ui.nav.RouteLastAdded
import remix.myplayer.ui.nav.RouteSetting
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.util.Constants

private val drawerExtraTitles = mutableListOf(
  R.string.drawer_history,
  R.string.drawer_recently_add,
  R.string.drawer_setting,
  R.string.exit
)

private val drawerExtraIcons = mutableListOf(
  R.drawable.ic_history_24dp,
  R.drawable.ic_recent_24dp,
  R.drawable.ic_settings_24dp,
  R.drawable.ic_exit_to_app_24dp
)

private fun libraryIcon(tag: Int): Int = when (tag) {
  Library.TAG_SONG -> R.drawable.ic_library_music_24dp
  Library.TAG_ALBUM -> R.drawable.ic_album_24dp
  Library.TAG_ARTIST -> R.drawable.ic_audio_file_24dp
  Library.TAG_GENRE -> R.drawable.ic_tune_24dp
  Library.TAG_PLAYLIST -> R.drawable.ic_format_list_bulleted_white_24dp
  Library.TAG_FOLDER -> R.drawable.ic_folder_24dp
  Library.TAG_REMOTE -> R.drawable.ic_smart_display_24dp
  else -> R.drawable.ic_library_music_24dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Drawer(
  modifier: Modifier = Modifier,
  libraries: List<Library>,
  selectedIndex: Int,
  onLibrarySelected: (Int) -> Unit,
  onClose: () -> Unit
) {
  val navController = LocalNavController.current
  val context = LocalContext.current
  val theme = LocalTheme.current

  val drawerDefault = colorResource(
    when (theme.theme) {
      LIGHT -> R.color.drawer_default_light
      DARK -> R.color.drawer_default_dark
      BLACK -> R.color.drawer_default_black
      else -> throw IllegalArgumentException("unknown theme: $theme")
    }
  )
  val drawerEffect = colorResource(
    when (theme.theme) {
      LIGHT -> R.color.drawer_effect_light
      DARK -> R.color.drawer_effect_dark
      BLACK -> R.color.drawer_effect_black
      else -> throw IllegalArgumentException("unknown theme: $theme")
    }
  )

  Column(modifier = modifier.background(drawerDefault)) {
    TopAppBar(
      title = {},
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = theme.primary,
        scrolledContainerColor = theme.primary,
        navigationIconContentColor = Color.White,
        actionIconContentColor = Color.White,
      )
    )

    LazyColumn(modifier = Modifier.weight(1f)) {
      // 库导航项
      itemsIndexed(libraries) { index, library ->
        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(library.stringRes),
              fontSize = 16.sp
            )
          },
          selected = index == selectedIndex,
          onClick = {
            onLibrarySelected(index)
            onClose()
          },
          icon = {
            Icon(
              modifier = Modifier.padding(start = 8.dp),
              painter = painterResource(libraryIcon(library.tag)),
              contentDescription = null,
              tint = theme.primary
            )
          },
          shape = RectangleShape,
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = drawerEffect,
            unselectedContainerColor = drawerDefault
          )
        )
      }

      // 分隔线
      item {
        HorizontalDivider(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          thickness = 1.dp,
          color = drawerEffect
        )
      }

      // 其他导航项
      itemsIndexed(drawerExtraTitles) { index, item ->
        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(drawerExtraTitles[index]),
              fontSize = 16.sp
            )
          },
          selected = false,
          onClick = {
            when (item) {
              R.string.drawer_history -> {
                navController.navigate(RouteHistory)
                onClose()
              }
              R.string.drawer_recently_add -> {
                navController.navigate(RouteLastAdded)
                onClose()
              }
              R.string.drawer_setting -> {
                navController.navigate(RouteSetting)
                onClose()
              }
              R.string.exit -> {
                context.sendBroadcast(
                  Intent(Constants.ACTION_EXIT)
                    .setComponent(ComponentName(context, ExitReceiver::class.java))
                )
                onClose()
              }
            }
          },
          icon = {
            Icon(
              modifier = Modifier.padding(start = 8.dp),
              painter = painterResource(drawerExtraIcons[index]),
              contentDescription = null,
              tint = theme.primary
            )
          },
          shape = RectangleShape,
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = drawerEffect,
            unselectedContainerColor = drawerDefault
          )
        )
      }
    }
  }
}
