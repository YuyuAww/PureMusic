package remix.myplayer.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.model.misc.Library
import remix.myplayer.data.prefs.SettingPrefs
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
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.smbViewModel
import remix.myplayer.viewmodel.webDavViewModel

private enum class DrawerAnchor { Closed, Open }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen() {
  val mainVM = mainViewModel
  val libraryVM = libraryViewModel

  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()

  val density = LocalDensity.current
  val drawerWidth = (LocalConfiguration.current.screenWidthDp * 0.45f).dp
  val drawerWidthPx = with(density) { drawerWidth.toPx() }

  val drawerState = remember {
    AnchoredDraggableState(
      initialValue = DrawerAnchor.Closed,
      positionalThreshold = { distance -> distance * 0.5f },
      velocityThreshold = { with(density) { 125.dp.toPx() } },
      snapAnimationSpec = tween(),
      decayAnimationSpec = androidx.compose.animation.core.exponentialDecay()
    ).also { state ->
      state.updateAnchors(
        DraggableAnchors {
          DrawerAnchor.Closed at 0f
          DrawerAnchor.Open at drawerWidthPx
        }
      )
    }
  }
  val scope = rememberCoroutineScope()

  val isDrawerOpen by remember {
    derivedStateOf { drawerState.currentValue == DrawerAnchor.Open }
  }

  val openDrawer: () -> Unit = {
    scope.launch { drawerState.animateTo(DrawerAnchor.Open) }
  }
  val closeDrawer: () -> Unit = {
    scope.launch { drawerState.animateTo(DrawerAnchor.Closed) }
  }

  BackHandler(enabled = isDrawerOpen || multiSelectState.isShowing()) {
    if (isDrawerOpen) {
      closeDrawer()
    } else if (multiSelectState.isShowing()) {
      mainVM.closeMultiSelect()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .anchoredDraggable(drawerState, Orientation.Horizontal)
  ) {
    val libraries by settingViewModel.enabledLibraries.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { libraries.size }

    // 侧边栏：从左侧滑入 (translationX: -drawerWidth → 0)
    Drawer(
      modifier = Modifier
        .width(drawerWidth)
        .fillMaxHeight()
        .offset {
          IntOffset(
            x = (drawerState.requireOffset() - drawerWidthPx).toInt(),
            y = 0
          )
        },
      libraries = libraries,
      selectedIndex = pagerState.currentPage,
      onLibrarySelected = { index ->
        scope.launch { pagerState.animateScrollToPage(index) }
      },
      onClose = closeDrawer
    )

    // 主页内容：向右平移 (translationX: 0 → drawerWidth)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .offset {
          IntOffset(
            x = drawerState.requireOffset().toInt(),
            y = 0
          )
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
  val multiSelectState by mainViewModel.multiSelectState.collectAsStateWithLifecycle()
  val showMultiSelect by remember {
    derivedStateOf { multiSelectState.isShowInLibrary() }
  }

  Column(modifier = Modifier.padding(contentPadding)) {
    // 多选模式下隐藏第二行，避免顶栏与列表之间出现多余控件
    if (!showMultiSelect) {
      ShuffleSortHeader(currentLibrary)
    }

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
  // 远程库：无第二行
  if (library.tag == Library.TAG_REMOTE) {
    return
  }

  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val mainVM = mainViewModel
  val activeColor = LocalTheme.current.secondary
  val inactiveColor = colorResource(R.color.default_model_button_color)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .background(LocalTheme.current.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    when (library.tag) {
      Library.TAG_SONG -> {
        // 左：随机播放全部
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
              tint = activeColor,
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
        // 右：长按多选 + 排序
        IconButton(onClick = {
          mainVM.startMultiSelect(MultiSelectState.Where.Song)
        }) {
          Icon(
            painter = painterResource(R.drawable.ic_checklist_white_24dp),
            contentDescription = "MultiSelect"
          )
        }
        ScreenPopupButton(library)
      }
      Library.TAG_ALBUM, Library.TAG_ARTIST, Library.TAG_GENRE, Library.TAG_PLAYLIST -> {
        val mode = when (library.tag) {
          Library.TAG_ALBUM -> settingState.library.albumMode
          Library.TAG_ARTIST -> settingState.library.artistMode
          Library.TAG_GENRE -> settingState.library.genreMode
          else -> settingState.library.playlistMode
        }
        val grid = mode == SettingPrefs.GRID_MODE
        val setMode: (Int) -> Unit = { target ->
          when (library.tag) {
            Library.TAG_ALBUM -> settingVM.setAlbumMode(target)
            Library.TAG_ARTIST -> settingVM.setArtistMode(target)
            Library.TAG_GENRE -> settingVM.setGenreMode(target)
            Library.TAG_PLAYLIST -> settingVM.setPlaylistMode(target)
          }
        }
        // 左：平铺/封面 切换
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            modifier = Modifier
              .padding(start = 16.dp)
              .clickableWithoutRipple(interactionSource = remember { MutableInteractionSource() }) {
                setMode(SettingPrefs.GRID_MODE)
              },
            painter = painterResource(R.drawable.ic_apps_white_24dp),
            contentDescription = "ModeGrid",
            tint = if (grid) activeColor else inactiveColor
          )
          Icon(
            modifier = Modifier
              .padding(horizontal = 18.dp)
              .clickableWithoutRipple(interactionSource = remember { MutableInteractionSource() }) {
                setMode(SettingPrefs.LIST_MODE)
              },
            painter = painterResource(R.drawable.ic_format_list_bulleted_white_24dp),
            contentDescription = "ModeList",
            tint = if (!grid) activeColor else inactiveColor
          )
        }
        // 右：排序
        ScreenPopupButton(library)
      }
      else -> {
        // 文件夹等：仅排序
        Spacer(Modifier.weight(1f))
        ScreenPopupButton(library)
      }
    }
  }
}


