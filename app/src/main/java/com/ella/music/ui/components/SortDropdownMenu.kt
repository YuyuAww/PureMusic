package com.ella.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.ui.listmodel.SortDirection
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SortDropdownItem(
    val text: String,
    val selected: Boolean,
    val summary: String? = null,
    val onClick: () -> Unit,
    val direction: SortDirection? = null,
    val onSelectAscending: (() -> Unit)? = null,
    val onSelectDescending: (() -> Unit)? = null
)

internal data class DirectionalSortField<T>(
    val field: T,
    val text: String,
    val defaultDirection: SortDirection = SortDirection.Ascending,
    val supportsAscending: Boolean = true,
    val supportsDescending: Boolean = true
)

/**
 * Adapts screens which still persist their sort selection as an enum with separate ascending and
 * descending entries. Keeping that persisted representation avoids a settings migration, while
 * presenting every such pair as one Miuix row with the direction controls on the right.
 */
internal data class DirectionalSortModeField<M>(
    val text: String,
    val ascendingMode: M? = null,
    val descendingMode: M? = null
)

internal fun <T> directionalSortDropdownItems(
    fields: List<DirectionalSortField<T>>,
    selectedField: T,
    selectedDirection: SortDirection,
    ascendingSummary: String,
    descendingSummary: String,
    onSelect: (field: T, direction: SortDirection) -> Unit
): List<SortDropdownItem> =
    fields.map { option ->
        val selected = option.field == selectedField
        SortDropdownItem(
            text = option.text,
            selected = selected,
            summary = if (selected) {
                if (selectedDirection == SortDirection.Descending) descendingSummary else ascendingSummary
            } else {
                null
            },
            direction = if (selected) selectedDirection else option.defaultDirection,
            onSelectAscending = option.supportsAscending.takeIf { it }?.let {
                { onSelect(option.field, SortDirection.Ascending) }
            },
            onSelectDescending = option.supportsDescending.takeIf { it }?.let {
                { onSelect(option.field, SortDirection.Descending) }
            },
            onClick = {
                onSelect(option.field, if (selected) selectedDirection else option.defaultDirection)
            }
        )
    }

internal fun <M> directionalSortModeDropdownItems(
    fields: List<DirectionalSortModeField<M>>,
    selectedMode: M,
    onSelect: (M) -> Unit
): List<SortDropdownItem> =
    fields.mapNotNull { option ->
        val ascendingMode = option.ascendingMode
        val descendingMode = option.descendingMode
        if (ascendingMode == null && descendingMode == null) return@mapNotNull null
        val selectedDirection = when (selectedMode) {
            descendingMode -> SortDirection.Descending
            else -> SortDirection.Ascending
        }
        val selected = selectedMode == ascendingMode || selectedMode == descendingMode
        val defaultMode = ascendingMode ?: descendingMode!!
        SortDropdownItem(
            text = option.text,
            selected = selected,
            direction = if (selected) selectedDirection else {
                if (ascendingMode != null) SortDirection.Ascending else SortDirection.Descending
            },
            onSelectAscending = ascendingMode?.let { mode -> { onSelect(mode) } },
            onSelectDescending = descendingMode?.let { mode -> { onSelect(mode) } },
            onClick = { onSelect(if (selected) selectedMode else defaultMode) }
        )
    }

@Composable
internal fun SortDropdownMenu(
    items: List<SortDropdownItem>,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    contentDescription: String = stringResource(R.string.common_sort)
) {
    var sheetVisible by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { sheetVisible = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MiuixIcons.Regular.Sort,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
    SortBottomSheet(
        show = sheetVisible,
        items = items,
        onDismissRequest = { sheetVisible = false }
    )
}

@Composable
private fun SortBottomSheet(
    show: Boolean,
    items: List<SortDropdownItem>,
    onDismissRequest: () -> Unit
) {
    EllaMiuixBottomSheet(
        show = show,
        title = stringResource(R.string.common_sort),
        onDismissRequest = onDismissRequest
    ) {
        EllaMiuixSheetColumn(
            maxHeight = 680.dp,
            showHandle = false,
            spacing = 8.dp,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items.forEach { item ->
                SortBottomSheetItem(
                    item = item,
                    onApplied = onDismissRequest
                )
            }
        }
    }
}

@Composable
private fun SortBottomSheetItem(
    item: SortDropdownItem,
    onApplied: () -> Unit
) {
    val hasDirectionChoices = item.onSelectAscending != null || item.onSelectDescending != null
    val containerColor = if (item.selected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val titleColor = if (item.selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    item.onClick()
                    onApplied()
                }
                .padding(vertical = 3.dp)
        ) {
            Text(
                text = item.text,
                fontSize = 16.sp,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.summary?.takeIf { !hasDirectionChoices }?.let { summary ->
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Keep both direction affordances visible for every sortable field.  Only the active
        // field is accented, so inactive arrows remain discoverable without appearing selected.
        if (hasDirectionChoices) {
            SortDirectionAction(
                selected = item.selected && item.direction == SortDirection.Ascending,
                onClick = item.onSelectAscending?.let { select ->
                    {
                        select()
                        onApplied()
                    }
                },
                ascending = true,
                contentDescription = stringResource(R.string.common_sort_ascending)
            )
            SortDirectionAction(
                selected = item.selected && item.direction == SortDirection.Descending,
                onClick = item.onSelectDescending?.let { select ->
                    {
                        select()
                        onApplied()
                    }
                },
                ascending = false,
                contentDescription = stringResource(R.string.common_sort_descending)
            )
        } else if (!hasDirectionChoices) {
            SelectionCheck(
                selected = item.selected,
                size = 22.dp,
                selectedColor = MiuixTheme.colorScheme.primary,
                unselectedColor = MiuixTheme.colorScheme.surfaceContainer
            )
        }
    }
}

@Composable
private fun SortDirectionAction(
    selected: Boolean,
    onClick: (() -> Unit)?,
    ascending: Boolean,
    contentDescription: String
) {
    val enabled = onClick != null
    val background = when {
        selected -> MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
        enabled -> Color.Transparent
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> MiuixTheme.colorScheme.primary
        enabled -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.72f)
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.28f)
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .background(background)
            .clickable(enabled = enabled) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = MiuixIcons.Regular.Back,
            contentDescription = contentDescription,
            tint = when {
                selected -> MiuixTheme.colorScheme.primary
                enabled -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                else -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.30f)
            },
            // Miuix's back glyph has the rounded shaft and arrow head used throughout the app.
            // Rotating it keeps the ascending/descending controls visually native instead of
            // using the old corner-shaped expand glyphs.
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = if (ascending) 90f else -90f }
        )
    }
}

@Composable
internal fun SortDropdownMenuContent(
    items: List<SortDropdownItem>,
    content: @Composable () -> Unit
) {
    var sheetVisible by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.clickable { sheetVisible = true }
    ) {
        content()
    }
    SortBottomSheet(
        show = sheetVisible,
        items = items,
        onDismissRequest = { sheetVisible = false }
    )
}
