/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(ExperimentalFoundationApi::class)

package ch.protonmail.android.mailsidebar.presentation.label

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.domain.model.LabelType.MessageFolder
import ch.protonmail.android.maillabel.domain.model.LabelType.MessageLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.testTag
import ch.protonmail.android.mailsidebar.presentation.R
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarItem
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarLazy
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Add
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Collapse
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Expand
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Select

fun LazyListScope.sidebarLabelItems(items: List<MailLabelUiModel.Custom>, onLabelAction: (SidebarLabelAction) -> Unit) =
    sidebarCustomLabelItems(MessageLabel, items, onLabelAction)

fun LazyListScope.sidebarFolderItems(
    items: List<MailLabelUiModel.Custom>,
    onLabelAction: (SidebarLabelAction) -> Unit
) = sidebarCustomLabelItems(MessageFolder, items, onLabelAction)

private fun LazyListScope.sidebarCustomLabelItems(
    type: LabelType,
    items: List<MailLabelUiModel.Custom>,
    onLabelAction: (SidebarLabelAction) -> Unit
) {
    items(items = items.filter { it.isVisible }, key = { it.id.labelId.id.plus(type) }) {
        SidebarCustomLabel(it, onLabelAction)
    }
    item {
        SidebarCustomLabelTitleItem(
            type = type,
            onAddClick = { onLabelAction(Add(type)) }
        )
    }
}

@Composable
private fun LazyItemScope.SidebarCustomLabel(
    item: MailLabelUiModel.Custom,
    onLabelAction: (SidebarLabelAction) -> Unit
) {
    SidebarItemWithCounter(
        modifier = Modifier
            .testTag("${SidebarCustomLabelTestTags.RootItem}${item.testTag}")
            .animateItem(),
        iconModifier = Modifier
            .testTag(SidebarCustomLabelTestTags.Icon)
            .semantics { tintColor = item.iconTint }
            .padding(start = item.iconPaddingStart),
        textModifier = Modifier
            .testTag(SidebarCustomLabelTestTags.Text),
        icon = painterResource(item.icon),
        text = item.text.value,
        iconTint = item.iconTint ?: ProtonTheme.colors.iconWeak,
        isSelected = item.isSelected,
        count = item.count,
        showChevron = item.hasChildren,
        isExpanded = item.isExpanded,
        onChevronClick = {
            onLabelAction(if (item.isExpanded) Collapse(item.id) else Expand(item.id))
        },
        onClick = { onLabelAction(Select(item.id)) }
    )
}

@Composable
private fun SidebarCustomLabelTitleItem(type: LabelType, onAddClick: () -> Unit) {
    val textRes = when (type) {
        MessageFolder -> R.string.drawer_item_create_folder
        MessageLabel -> R.string.drawer_item_create_label
        else -> return
    }

    ProtonSidebarItem(
        text = textRes,
        icon = R.drawable.ic_proton_plus,
        onClick = onAddClick,
        isClickable = true,
        isSelected = false
    )
}

@Preview(
    name = "Sidebar Labels in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Sidebar Labels in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
@Suppress("MagicNumber")
private fun PreviewSidebarLabelFolderItems() {
    ProtonTheme {
        ProtonSidebarLazy {
            sidebarFolderItems(
                items = listOf(
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder1")),
                        text = TextUiModel.Text("Folder1"),
                        icon = R.drawable.ic_proton_folders_filled,
                        iconTint = Color.Blue,
                        isSelected = false,
                        count = CappedNumberUiModel.Exact(1),
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder2")),
                        text = TextUiModel.Text("Folder2"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Red,
                        isSelected = true,
                        count = CappedNumberUiModel.Exact(2),
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = ProtonDimens.Spacing.Large * 1
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder3")),
                        text = TextUiModel.Text("Folder3"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Yellow,
                        isSelected = false,
                        count = CappedNumberUiModel.Empty,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = ProtonDimens.Spacing.Large * 2
                    )
                ),
                onLabelAction = {}
            )
            item { HorizontalDivider() }
            sidebarLabelItems(
                items = listOf(
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Label(LabelId("label1")),
                        text = TextUiModel.Text("Label1"),
                        icon = R.drawable.ic_proton_circle_filled,
                        iconTint = Color.Green,
                        isSelected = false,
                        count = CappedNumberUiModel.Zero,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Label(LabelId("label2")),
                        text = TextUiModel.Text("Label2"),
                        icon = R.drawable.ic_proton_circle_filled,
                        iconTint = Color.Cyan,
                        isSelected = false,
                        count = CappedNumberUiModel.Exact(9),
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    )
                ),
                onLabelAction = {}
            )
            item { HorizontalDivider() }
            sidebarFolderItems(
                items = emptyList(),
                onLabelAction = {}
            )
            item { HorizontalDivider() }
            sidebarLabelItems(
                items = emptyList(),
                onLabelAction = {}
            )
        }
    }
}

object SidebarCustomLabelTestTags {

    const val RootItem = "SidebarItem"
    const val Icon = "SidebarIcon"
    const val Text = "SidebarText"
}
