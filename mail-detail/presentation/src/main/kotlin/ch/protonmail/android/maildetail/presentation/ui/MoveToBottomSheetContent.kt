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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.maillabel.presentation.textRes
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.interactionNorm
import me.proton.core.label.domain.entity.LabelId

@Composable
fun MoveToBottomSheetContent(
    state: MoveToBottomSheetState,
    onFolderSelected: (MailLabelId) -> Unit,
    onDoneClick: (String) -> Unit
) {
    when (state) {
        is MoveToBottomSheetState.Data -> MoveToBottomSheetContent(state, onFolderSelected, onDoneClick)
        else -> ProtonCenteredProgress()
    }
}

@Composable
fun MoveToBottomSheetContent(
    dataState: MoveToBottomSheetState.Data,
    onFolderSelected: (MailLabelId) -> Unit,
    onDoneClick: (String) -> Unit
) {
    val selectedMailLabel = dataState.selected?.text?.string()
    Column {
        Row(
            modifier = Modifier
                .testTag(MoveToBottomSheetTestTags.RootItem)
                .padding(ProtonDimens.DefaultSpacing)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.testTag(MoveToBottomSheetTestTags.MoveToText),
                text = stringResource(id = R.string.bottom_sheet_move_to_title),
                style = ProtonTheme.typography.default
            )
            Text(
                modifier = Modifier
                    .testTag(MoveToBottomSheetTestTags.DoneButton)
                    .semantics { selectedMailLabel ?: disabled() }
                    .clickable { selectedMailLabel?.let(onDoneClick) },
                text = stringResource(id = R.string.bottom_sheet_done_action),
                style = ProtonTheme.typography.default,
                color = ProtonTheme.colors.interactionNorm(dataState.selected != null)
            )
        }
        Divider(modifier = Modifier.testTag(MoveToBottomSheetTestTags.Divider))
        LazyColumn {
            items(dataState.moveToDestinations) {
                ProtonRawListItem(
                    modifier = Modifier
                        .testTag(MoveToBottomSheetTestTags.FolderItem)
                        .clickable { onFolderSelected(it.id) }
                        .height(ProtonDimens.ListItemHeight)
                        .padding(end = ProtonDimens.DefaultSpacing)
                ) {
                    Icon(
                        modifier = Modifier
                            .testTag(MoveToBottomSheetTestTags.FolderIcon)
                            .semantics { tintColor = it.iconTint }
                            .padding(start = if (it is MailLabelUiModel.Custom) it.iconPaddingStart else 0.dp)
                            .padding(horizontal = ProtonDimens.DefaultSpacing),
                        painter = painterResource(id = it.icon),
                        contentDescription = NO_CONTENT_DESCRIPTION,
                        tint = it.iconTint ?: ProtonTheme.colors.iconWeak
                    )
                    Text(
                        modifier = Modifier
                            .testTag(MoveToBottomSheetTestTags.FolderNameText)
                            .weight(1f),
                        text = it.text.string(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(
                        modifier = Modifier
                            .testTag(MoveToBottomSheetTestTags.FolderSpacer)
                            .size(ProtonDimens.SmallSpacing)
                    )
                    if (it.isSelected) {
                        Icon(
                            modifier = Modifier
                                .testTag(MoveToBottomSheetTestTags.FolderSelectionIcon)
                                .padding(end = ProtonDimens.SmallSpacing)
                                .size(ProtonDimens.SmallIconSize),
                            painter = painterResource(id = R.drawable.ic_proton_checkmark),
                            contentDescription = NO_CONTENT_DESCRIPTION,
                            tint = ProtonTheme.colors.interactionNorm(true)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MoveToBottomSheetContentPreview() {
    MoveToBottomSheetContent(
        dataState = MoveToBottomSheetState.Data(
            selected = null,
            moveToDestinations = listOf(
                MailLabelUiModel.System(
                    id = MailLabelId.System.Spam,
                    text = TextUiModel.TextRes(MailLabelId.System.Spam.systemLabelId.textRes()),
                    icon = MailLabelId.System.Spam.systemLabelId.iconRes(),
                    iconTint = null,
                    isSelected = true,
                    count = null
                ),
                MailLabelUiModel.Custom(
                    id = MailLabelId.Custom.Folder(LabelId("folder1")),
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
                    text = TextUiModel.Text("Folder3"),
                    icon = R.drawable.ic_proton_folder_filled,
                    iconTint = Color.Yellow,
                    isSelected = false,
                    count = null,
                    isVisible = true,
                    isExpanded = true,
                    iconPaddingStart = ProtonDimens.DefaultSpacing * 2
                ),
                MailLabelUiModel.Custom(
                    id = MailLabelId.Custom.Folder(LabelId("really long folder name")),
                    text = TextUiModel.Text("THis folder is really long so that truncation can be tested"),
                    icon = R.drawable.ic_proton_folders_filled,
                    iconTint = Color.Blue,
                    isSelected = true,
                    count = 1,
                    isVisible = true,
                    isExpanded = true,
                    iconPaddingStart = 0.dp
                )
            ).toImmutableList()
        ),
        onFolderSelected = {},
        onDoneClick = {}
    )
}

object MoveToBottomSheetTestTags {

    const val RootItem = "MoveToBottomSheetRootItem"
    const val MoveToText = "MoveToText"
    const val DoneButton = "DoneButton"
    const val Divider = "MoveToBottomSheetDivider"
    const val FolderItem = "FolderItem"
    const val FolderIcon = "FolderIcon"
    const val FolderNameText = "FolderNameText"
    const val FolderSpacer = "FolderSpacer"
    const val FolderSelectionIcon = "FolderSelectionIcon"
}
