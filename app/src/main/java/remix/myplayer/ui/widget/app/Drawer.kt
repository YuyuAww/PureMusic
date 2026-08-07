package remix.myplayer.ui.widget.app

import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import remix.myplayer.R
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

@Composable
fun Drawer(drawerState: DrawerState) {
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

  ModalDrawerSheet(
    modifier = Modifier
      .width(256.dp)
      .fillMaxHeight(),
    drawerShape = RectangleShape,
    drawerContainerColor = drawerDefault,
    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Start)
  ) {
    // 顶部 Header 与主页 TopAppBar 对齐：windowInsetsPadding 自动处理状态栏，64dp 对应 TopAppBar 高度
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(theme.primary)
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        .height(64.dp)
    )

    val scope = rememberCoroutineScope()
    LazyColumn(modifier = Modifier.background(drawerDefault)) {
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
              R.string.drawer_song -> scope.launch { drawerState.close() }
              // 历史
              R.string.drawer_history -> navController.navigate(RouteHistory)
              // 最近添加
              R.string.drawer_recently_add -> navController.navigate(RouteLastAdded)
              // 设置
              R.string.drawer_setting -> navController.navigate(RouteSetting)
              // 退出
              R.string.exit -> {
                context.sendBroadcast(
                  Intent(Constants.ACTION_EXIT)
                    .setComponent(ComponentName(context, ExitReceiver::class.java))
                )
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

      item {
        Spacer(modifier = Modifier.weight(1f))
      }
    }

  }
}
