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

package ch.protonmail.android.maillabel.presentation.sidebar

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.labelAddTitleRes
import ch.protonmail.android.maillabel.presentation.labelTitleRes
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Add
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Select
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.ViewList
import ch.protonmail.android.maillabel.presentation.testTag
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.LabelType.MessageFolder
import me.proton.core.label.domain.entity.LabelType.MessageLabel

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
    item {
        SidebarCustomLabelTitleItem(
            type = type,
            showAddIcon = items.isNotEmpty(),
            onAddClick = { onLabelAction(Add(type)) },
            onLabelsClick = { onLabelAction(ViewList(type)) }
        )
    }
    when {
        items.isEmpty() -> item { SidebarAddCustomLabelItem(type) { onLabelAction(Add(type)) } }
        else -> items(items = items.filter { it.isVisible }, key = { it.id.labelId.id }) {
            SidebarCustomLabel(it, onLabelAction)
        }
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
            .animateItemPlacement(),
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
        onClick = { onLabelAction(Select(item.id)) }
    )
}

@Composable
private fun SidebarCustomLabelTitleItem(
    type: LabelType,
    showAddIcon: Boolean,
    onAddClick: () -> Unit,
    onLabelsClick: () -> Unit
) {
    ProtonSidebarItem(
        modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
        isClickable = showAddIcon,
        onClick = onLabelsClick
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = true),
            text = stringResource(type.labelTitleRes()),
            color = ProtonTheme.colors.textWeak
        )
        if (showAddIcon) {
            IconButton(
                onClick = onAddClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_proton_plus),
                    contentDescription = stringResource(
                        id = if (type == MessageLabel) R.string.label_title_create_label
                        else R.string.label_title_create_folder
                    ),
                    tint = ProtonTheme.colors.iconWeak
                )
            }
        }
    }
}

@Composable
private fun SidebarAddCustomLabelItem(type: LabelType, onClick: () -> Unit) {
    ProtonSidebarItem(
        isClickable = true,
        onClick = onClick,
        icon = R.drawable.ic_proton_plus,
        text = type.labelAddTitleRes()
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
private fun PreviewSidebarLabelFolderItems() {
    ProtonTheme {
        ProtonSidebarLazy {
            sidebarFolderItems(
                items = listOf(
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder1")),
                        key = "k1",
                        text = TextUiModel.Text("Folder1"),
                        icon = R.drawable.ic_proton_folders_filled,
                        iconTint = Color.Blue,
                        isSelected = false,
                        count = 1,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder2")),
                        key = "k2",
                        text = TextUiModel.Text("Folder2"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Red,
                        isSelected = true,
                        count = 2,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = ProtonDimens.DefaultSpacing * 1
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder3")),
                        key = "k3",
                        text = TextUiModel.Text("Folder3"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Yellow,
                        isSelected = false,
                        count = null,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = ProtonDimens.DefaultSpacing * 2
                    )
                ),
                onLabelAction = {}
            )
            item { Divider() }
            sidebarLabelItems(
                items = listOf(
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Label(LabelId("label1")),
                        key = "k4",
                        text = TextUiModel.Text("Label1"),
                        icon = R.drawable.ic_proton_circle_filled,
                        iconTint = Color.Green,
                        isSelected = false,
                        count = 0,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    ),
                    MailLabelUiModel.Custom(
                        id = MailLabelId.Custom.Label(LabelId("label2")),
                        key = "k5",
                        text = TextUiModel.Text("Label2"),
                        icon = R.drawable.ic_proton_circle_filled,
                        iconTint = Color.Cyan,
                        isSelected = false,
                        count = 9,
                        isVisible = true,
                        isExpanded = true,
                        iconPaddingStart = 0.dp
                    )
                ),
                onLabelAction = {}
            )
            item { Divider() }
            sidebarFolderItems(
                items = emptyList(),
                onLabelAction = {}
            )
            item { Divider() }
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
