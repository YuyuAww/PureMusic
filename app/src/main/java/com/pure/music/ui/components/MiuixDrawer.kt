package com.pure.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ModalNavigationDrawer
import androidx.compose.foundation.ModalNavigationDrawerState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 抽屉条目：图标 + 标题，选中项以强调色区分 */
data class MiuixDrawerItem(
    val icon: ImageVector,
    val title: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * 应用自有的 Miuix 风格侧边抽屉。
 * Miuix v0.9.4 没有官方 Drawer 组件，这里用 Compose Foundation 的
 * [ModalNavigationDrawer] 承载交互，内部行、文案、配色全部使用 Miuix 组件。
 */
@Composable
fun MiuixDrawer(
    drawerState: ModalNavigationDrawerState,
    items: List<MiuixDrawerItem>,
    content: @Composable (PaddingValues) -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent(items) },
        content = content,
    )
}

@Composable
private fun DrawerContent(items: List<MiuixDrawerItem>) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 320.dp)
            .fillMaxHeight()
            .background(color = colors.surfaceContainer)
            .padding(start = 12.dp, top = 40.dp),
    ) {
        Text(
            text = "PureMusic",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 16.dp, bottom = 24.dp),
        )
        Column(Modifier.verticalScroll(rememberScrollState())) {
            items.forEach { item ->
                DrawerItemRow(item)
            }
        }
    }
}

@Composable
private fun DrawerItemRow(item: MiuixDrawerItem) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (item.selected) colors.primary else colors.onSurfaceVariantActions,
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (item.selected) colors.primary else colors.onSurface,
        )
    }
}
