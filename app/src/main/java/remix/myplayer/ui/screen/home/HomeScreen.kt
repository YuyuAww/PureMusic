package remix.myplayer.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.model.misc.Library
import remix.myplayer.service.Command
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.dialog.CreatePlayListDialog
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.Drawer
import remix.myplayer.ui.widget.app.FAButton
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.app.ViewPager
import remix.myplayer.ui.widget.popup.ScreenPopupButton
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ext.clickableWithoutRipple
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.smbViewModel
import remix.myplayer.viewmodel.webDavViewModel

private val DrawerWidth = 224.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen() {
  val mainVM = mainViewModel
  val libraryVM = libraryViewModel

  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()

  val drawerWidthPx = with(LocalDensity.current) { DrawerWidth.toPx() }
  val drawerOffset = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  var isDrawerOpen by remember { mutableStateOf(false) }

  val openDrawer: () -> Unit = {
    isDrawerOpen = true
    scope.launch { drawerOffset.animateTo(1f) }
  }
  val closeDrawer: () -> Unit = {
    isDrawerOpen = false
    scope.launch { drawerOffset.animateTo(0f) }
  }

  BackHandler(enabled = isDrawerOpen || multiSelectState.isShowing()) {
    if (isDrawerOpen) {
      closeDrawer()
    } else if (multiSelectState.isShowing()) {
      mainVM.closeMultiSelect()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    val libraries by settingViewModel.enabledLibraries.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { libraries.size }

    // 侧边栏
    Drawer(
      modifier = Modifier
        .width(DrawerWidth)
        .fillMaxHeight(),
      libraries = libraries,
      selectedIndex = pagerState.currentPage,
      onLibrarySelected = { index ->
        scope.launch { pagerState.animateScrollToPage(index) }
      },
      onClose = closeDrawer
    )

    // 主页内容，通过 graphicsLayer 平移实现"推开"效果
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          translationX = drawerOffset.value * drawerWidthPx
        }
        .background(LocalTheme.current.libraryBackground)
    ) {
      val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        flingAnimationSpec = null,
        snapAnimationSpec = null
      )

      val showMultiSelect by remember {
        derivedStateOf {
          multiSelectState.isShowInLibrary()
        }
      }

      Scaffold(
        Modifier
          .fillMaxSize()
          .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = LocalTheme.current.libraryBackground,
        topBar = {
          AnimatedContent(
            targetState = showMultiSelect,
            transitionSpec = {
              if (targetState) {
                slideInVertically() togetherWith slideOutVertically { height -> height / 2 }
              } else {
                slideInVertically { height -> height } togetherWith slideOutVertically()
              }
            }
          ) { isMultiSelect ->
            if (!isMultiSelect) {
              HomeAppBar(scrollBehavior, openDrawer)
            } else {
              MultiSelectBar(
                state = multiSelectState,
                scrollBehavior = scrollBehavior,
              )
            }
          }
        },
        floatingActionButton = {
          val selectLibrary by remember(libraries) {
            derivedStateOf {
              libraries.getOrElse(pagerState.currentPage) { libraries.first() }
            }
          }

          CreatePlayListDialog()

          var showAddRemoteMenu by remember { mutableStateOf(false) }

          val webDavVM = webDavViewModel
          val smbVM = smbViewModel
          Column {
            if (showAddRemoteMenu) {
              DropdownMenu(
                expanded = true,
                containerColor = LocalTheme.current.dialogBackground,
                onDismissRequest = { showAddRemoteMenu = false }
              ) {
                DropdownMenuItem(
                  text = {
                    Text(
                      stringResource(R.string.webdav),
                      color = LocalTheme.current.textPrimary
                    )
                  },
                  onClick = {
                    showAddRemoteMenu = false
                    webDavVM.showAddWebDavDialog()
                  }
                )
                if (smbVM.supportSmb) {
                  SmbDropDownMenu(smbVM) {
                    showAddRemoteMenu = false
                    smbVM.showAddSmbDialog()
                  }
                }
              }
            }

            FAButton(
              selectLibrary.tag == Library.TAG_PLAYLIST || selectLibrary.tag == Library.TAG_REMOTE
            ) {
              if (mainVM.multiSelectState.value.isShowing()) {
                return@FAButton
              }

              if (selectLibrary.tag == Library.TAG_PLAYLIST) {
                libraryVM.showCreatePlaylistDialog()
              } else if (selectLibrary.tag == Library.TAG_REMOTE) {
                showAddRemoteMenu = true
              }
            }
          }

        })
      { contentPadding ->
        HomeContent(contentPadding, pagerState, libraries)
      }

      // 侧边栏打开时，点击主页区域关闭侧边栏
      if (isDrawerOpen) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures { closeDrawer() }
            }
        )
      }
    }
  }
}

@Composable
private fun HomeContent(
  contentPadding: PaddingValues,
  pagerState: PagerState,
  libraries: List<Library>,
) {
  val scope = rememberCoroutineScope()
  val scrollToCurrentEvent = remember { MutableSharedFlow<Unit>() }
  val currentLibrary by settingViewModel.currentLibrary.collectAsStateWithLifecycle()

  Column(modifier = Modifier.padding(contentPadding)) {
    // 随机播放 + 排序栏（替代原 TabRow 位置）
    ShuffleSortHeader(currentLibrary)

    ViewPager(
      modifier = Modifier.weight(1f),
      libraries = libraries,
      pagerState = pagerState,
      scrollToCurrentEvent = scrollToCurrentEvent
    )

    BottomBar()
  }
}

@Composable
private fun ShuffleSortHeader(library: Library) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .background(LocalTheme.current.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 随机播放全部：仅歌曲库显示
    if (library.tag == Library.TAG_SONG) {
      val songs by libraryViewModel.songs.collectAsStateWithLifecycle()
      if (songs.isNotEmpty()) {
        Row(
          modifier = Modifier
            .weight(1f)
            .clickableWithoutRipple(remember { MutableInteractionSource() }) {
              setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.SKIP_TO_NEXT, true))
            },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            painter = painterResource(R.drawable.ic_shuffle_white_24dp),
            tint = LocalTheme.current.secondary,
            contentDescription = "ShuffleAll"
          )
          Text(
            text = stringResource(R.string.play_random, songs.size),
            color = LocalTheme.current.textSecondary
          )
        }
      } else {
        Spacer(Modifier.weight(1f))
      }
    } else {
      Spacer(Modifier.weight(1f))
    }

    // 排序：非远程库显示
    if (library.tag != Library.TAG_REMOTE) {
      ScreenPopupButton(library)
    }
  }
}

// 修改tab最小宽度
fun hackTabMinWidth() {
  try {
    Class
      .forName("androidx.compose.material3.TabRowKt")
      .getDeclaredField("ScrollableTabRowMinimumTabWidth")
      .apply {
        isAccessible = true
      }.set(null, 72f)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}
