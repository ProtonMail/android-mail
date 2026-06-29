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

package ch.protonmail.android.maillabel.presentation.bottomsheet.moveto

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.maillabel.presentation.sample.MoveToInboxCategorySample
import ch.protonmail.android.maillabel.presentation.textRes
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun MoveToBottomSheetContent(
    dataState: MoveToState.Data,
    actions: MoveToBottomSheetContent.Actions,
    modifier: Modifier = Modifier
) {
    val entryPoint = dataState.entryPoint

    ConsumableLaunchedEffect(dataState.shouldDismissEffect) { dismissData ->
        actions.onMoveToComplete(dismissData.mailLabelText, dismissData.isCategory, entryPoint)
    }

    ConsumableTextEffect(dataState.errorEffect) {
        actions.onError(it)
        actions.onDismiss()
    }

    ConsumableTextEffect(dataState.messageEffect) {
        actions.onMessage(it)
        actions.onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MoveToBottomSheetTestTags.RootItem)
    ) {
        MoveToSheetTitle()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Large)
                .verticalScroll(rememberScrollState())
        ) {
            val destinations = buildList<MoveToBottomSheetDestinationUiModel> {
                dataState.inboxDestination?.let(::add)
                addAll(dataState.categoryDestinations)
                addAll(dataState.systemDestinations)
            }

            MoveToGroup(
                destinations = destinations,
                onFolderSelected = { folderId, folderName ->
                    actions.onFolderSelected(folderId, MailLabelText(folderName), entryPoint)
                },
                onCategorySelected = { categoryId, categoryLabelText ->
                    actions.onCategorySelected(categoryId, categoryLabelText, entryPoint)
                }
            )

            Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))

            CustomMoveToGroupWithActionButton(
                destinations = dataState.customDestinations,
                onFolderSelected = { folderId, folderName ->
                    actions.onFolderSelected(folderId, MailLabelText(folderName), entryPoint)
                },
                onAddClick = actions.onCreateNewFolderClick
            )

            BottomNavigationBarSpacer()
        }
    }
}

@Composable
private fun MoveToSheetTitle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ProtonDimens.Spacing.Small,
                vertical = ProtonDimens.Spacing.Large
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .testTag(MoveToBottomSheetTestTags.MoveToText)
                .weight(1f),
            text = stringResource(id = R.string.bottom_sheet_move_to_title),
            style = ProtonTheme.typography.titleLargeNorm,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MoveToGroup(
    modifier: Modifier = Modifier,
    destinations: List<MoveToBottomSheetDestinationUiModel>,
    onFolderSelected: (MailLabelId, String) -> Unit,
    onCategorySelected: (MailLabelId.Category, MailLabelText) -> Unit = { _, _ -> }
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            destinations.forEachIndexed { index, label ->
                MoveToGroupItem(
                    label = label,
                    onFolderClicked = { name ->
                        if (label is MoveToBottomSheetDestinationUiModel.Category) {
                            onCategorySelected(label.id, label.mailLabelText)
                        } else {
                            onFolderSelected(label.id, name)
                        }
                    }
                )

                if (index < destinations.lastIndex) {
                    HorizontalDivider(
                        thickness = MailDimens.DefaultBorder,
                        color = ProtonTheme.colors.separatorNorm
                    )
                }
            }
        }
    }
}

@Composable
fun CustomMoveToGroupWithActionButton(
    modifier: Modifier = Modifier,
    destinations: List<MoveToBottomSheetDestinationUiModel.Custom>,
    onFolderSelected: (MailLabelId, String) -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            destinations.forEachIndexed { _, label ->
                MoveToGroupItem(
                    label = label,
                    onFolderClicked = { folderName ->
                        onFolderSelected(label.id, folderName)
                    }
                )
                HorizontalDivider(
                    thickness = MailDimens.DefaultBorder,
                    color = ProtonTheme.colors.separatorNorm
                )
            }
            CreateFolderButton(onClick = onAddClick)
        }
    }
}

@Composable
private fun MoveToGroupItem(
    modifier: Modifier = Modifier,
    label: MoveToBottomSheetDestinationUiModel,
    onFolderClicked: (String) -> Unit
) {
    val iconPaddingStart = (label as? MoveToBottomSheetDestinationUiModel.Custom)?.iconPaddingStart ?: 0.dp
    val folderName = label.text.string()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = {
                onFolderClicked(folderName)
            })
            .padding(
                vertical = ProtonDimens.Spacing.Large,
                horizontal = ProtonDimens.Spacing.Large
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .padding(
                    start = iconPaddingStart,
                    end = ProtonDimens.Spacing.Large
                )
                .size(ProtonDimens.IconSize.Medium),
            painter = painterResource(id = label.icon),
            tint = label.iconTint ?: Color.Unspecified,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier.weight(1f),
            text = folderName,
            style = ProtonTheme.typography.bodyLargeNorm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateFolderButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .testTag(MoveToBottomSheetTestTags.AddFolderRow)
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                vertical = ProtonDimens.Spacing.Large,
                horizontal = ProtonDimens.Spacing.Large
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .testTag(MoveToBottomSheetTestTags.AddFolderIcon)
                .padding(end = ProtonDimens.Spacing.Large),
            painter = painterResource(id = R.drawable.ic_proton_plus),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier
                .testTag(MoveToBottomSheetTestTags.AddFolderText)
                .weight(1f),
            text = stringResource(id = R.string.label_title_create_folder),
            style = ProtonTheme.typography.bodyLargeNorm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


object MoveToBottomSheetContent {

    data class Actions(
        val onCreateNewFolderClick: () -> Unit,
        val onFolderSelected: (MailLabelId, MailLabelText, MoveToBottomSheetEntryPoint) -> Unit,
        val onCategorySelected: (MailLabelId.Category, MailLabelText, MoveToBottomSheetEntryPoint) -> Unit,
        val onDismiss: () -> Unit,
        val onError: (String) -> Unit,
        val onMessage: (String) -> Unit,
        val onMoveToComplete: (
            labelText: MailLabelText,
            isCategory: Boolean,
            entryPoint: MoveToBottomSheetEntryPoint
        ) -> Unit
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoveToBottomSheetContentPreview() {
    ProtonTheme {
        MoveToBottomSheetContent(
            dataState = MoveToState.Data(
                customDestinations = listOf(
                    MoveToBottomSheetDestinationUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder1")),
                        text = TextUiModel.Text("Folder1"),
                        icon = R.drawable.ic_proton_folders_filled,
                        iconTint = Color.Red,
                        iconPaddingStart = 0.dp
                    ),
                    MoveToBottomSheetDestinationUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder2")),
                        text = TextUiModel.Text("Folder2"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Red,
                        iconPaddingStart = ProtonDimens.Spacing.Large * 1
                    ),
                    MoveToBottomSheetDestinationUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("folder3")),
                        text = TextUiModel.Text("Folder3"),
                        icon = R.drawable.ic_proton_folder_filled,
                        iconTint = Color.Yellow,
                        iconPaddingStart = ProtonDimens.Spacing.Large * 2
                    ),
                    MoveToBottomSheetDestinationUiModel.Custom(
                        id = MailLabelId.Custom.Folder(LabelId("really long folder name")),
                        text = TextUiModel.Text("This folder is really long so that truncation can be tested"),
                        icon = R.drawable.ic_proton_folders_filled,
                        iconTint = Color.Blue,
                        iconPaddingStart = 0.dp
                    )
                ).toImmutableList(),
                systemDestinations = listOf(
                    MoveToBottomSheetDestinationUiModel.System(
                        id = MailLabelId.System(LabelId("spam")),
                        text = TextUiModel.TextRes(SystemLabelId.Spam.textRes()),
                        icon = SystemLabelId.Spam.iconRes(),
                        iconTint = null
                    ),
                    MoveToBottomSheetDestinationUiModel.System(
                        id = MailLabelId.System(LabelId("trash")),
                        text = TextUiModel.TextRes(SystemLabelId.Trash.textRes()),
                        icon = SystemLabelId.Trash.iconRes(),
                        iconTint = null
                    ),
                    MoveToBottomSheetDestinationUiModel.System(
                        id = MailLabelId.System(LabelId("archive")),
                        text = TextUiModel.TextRes(SystemLabelId.Archive.textRes()),
                        icon = SystemLabelId.Archive.iconRes(),
                        iconTint = null
                    )
                ).toImmutableList(),
                categoryDestinations = listOf(
                    MoveToInboxCategorySample.primary,
                    MoveToInboxCategorySample.social,
                    MoveToInboxCategorySample.promotions
                ).toImmutableList(),
                inboxDestination = null,
                entryPoint = MoveToBottomSheetEntryPoint.Conversation,
                shouldDismissEffect = Effect.empty(),
                errorEffect = Effect.empty()
            ),
            actions = MoveToBottomSheetContent.Actions(
                onCreateNewFolderClick = {},
                onFolderSelected = { _, _, _ -> },
                onCategorySelected = { _, _, _ -> },
                onDismiss = {},
                onError = { _ -> },
                onMessage = { _ -> },
                onMoveToComplete = { _, _, _ -> }
            )
        )
    }
}

object MoveToBottomSheetTestTags {

    const val RootItem = "MoveToBottomSheetRootItem"
    const val MoveToText = "MoveToText"
    const val DoneButton = "DoneButton"
    const val FolderItem = "FolderItem"
    const val FolderIcon = "FolderIcon"
    const val FolderNameText = "FolderNameText"
    const val FolderSelectionIcon = "FolderSelectionIcon"
    const val AddFolderRow = "AddFolderRow"
    const val AddFolderIcon = "AddFolderIcon"
    const val AddFolderText = "AddFolderText"
}
