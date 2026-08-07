package remix.myplayer.ui.widget.app

import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import remix.myplayer.ui.widget.popup.ScreenPopupButton
import remix.myplayer.util.Constants
import remix.myplayer.viewmodel.settingViewModel

private val drawerTitles = mutableListOf(
  R.string.drawer_song,
  R.string.drawer_history,
  R.string.drawer_recently_add,
  R.string.drawer_setting,
  R.string.exit
)

private val drawerIcons = mutableListOf(
  R.drawable.ic_library_music_24dp,
  R.drawable.ic_history_24dp,
  R.drawable.ic_recent_24dp,
  R.drawable.ic_settings_24dp,
  R.drawable.ic_exit_to_app_24dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Drawer(
  modifier: Modifier = Modifier,
  onClose: () -> Unit
) {
  val navController = LocalNavController.current
  val context = LocalContext.current
  val theme = LocalTheme.current
  val library by settingViewModel.currentLibrary.collectAsStateWithLifecycle()

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
    // 顶部 Header 使用与主页完全相同的 TopAppBar，保证顶部（状态栏高度+内容高度）完全对齐
    TopAppBar(
      title = {},
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = theme.primary,
        scrolledContainerColor = theme.primary,
        navigationIconContentColor = Color.White,
        actionIconContentColor = Color.White,
      ),
      actions = {
        if (library.tag != Library.TAG_REMOTE) {
          ScreenPopupButton(library)
        }
      }
    )

    LazyColumn(modifier = Modifier.weight(1f)) {
      itemsIndexed(drawerTitles) { index, item ->

        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(drawerTitles[index]),
              fontSize = 16.sp
            )
          },
          selected = index == 0,
          onClick = {
            when (item) {
              // 歌曲库
              R.string.drawer_song -> onClose()
              // 历史
              R.string.drawer_history -> {
                navController.navigate(RouteHistory)
                onClose()
              }
              // 最近添加
              R.string.drawer_recently_add -> {
                navController.navigate(RouteLastAdded)
                onClose()
              }
              // 设置
              R.string.drawer_setting -> {
                navController.navigate(RouteSetting)
                onClose()
              }
              // 退出
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
              painter = painterResource(drawerIcons[index]),
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
