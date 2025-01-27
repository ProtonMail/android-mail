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

package ch.protonmail.android.mailmessage.presentation.ui.bottomsheet

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
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.extension.tintColor
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelWithSelectedStateSample
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.interactionNorm
import me.proton.core.label.domain.entity.LabelId

@Composable
fun LabelAsBottomSheetContent(state: LabelAsBottomSheetState, actions: LabelAsBottomSheetContent.Actions) {
    when (state) {
        is LabelAsBottomSheetState.Data -> LabelAsBottomSheetContent(
            labelAsDataState = state,
            actions = actions
        )

        else -> ProtonCenteredProgress()
    }
}

@Composable
fun LabelAsBottomSheetContent(
    labelAsDataState: LabelAsBottomSheetState.Data,
    actions: LabelAsBottomSheetContent.Actions
) {
    var archiveSelectedState by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .testTag(LabelAsBottomSheetTestTags.RootItem)
                .padding(ProtonDimens.DefaultSpacing)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.testTag(LabelAsBottomSheetTestTags.LabelAsText),
                text = stringResource(id = R.string.bottom_sheet_label_as_title),
                style = ProtonTheme.typography.default
            )
            Text(
                modifier = Modifier
                    .testTag(LabelAsBottomSheetTestTags.DoneButton)
                    .clickable { actions.onDoneClick(archiveSelectedState, labelAsDataState.entryPoint) },
                text = stringResource(id = R.string.bottom_sheet_done_action),
                style = ProtonTheme.typography.default,
                color = ProtonTheme.colors.interactionNorm()
            )
        }
        Divider(modifier = Modifier.testTag(LabelAsBottomSheetTestTags.Divider))
        SettingsToggleItem(
            modifier = Modifier.testTag(LabelAsBottomSheetTestTags.AlsoArchiveToggle),
            name = stringResource(id = R.string.bottom_sheet_archive_action),
            value = archiveSelectedState,
            onToggle = { archiveSelectedState = !archiveSelectedState }
        )
        Row(
            modifier = Modifier
                .testTag(LabelAsBottomSheetTestTags.AddLabelRow)
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(id = R.string.create_label_content_description),
                    role = Role.Button,
                    onClick = actions.onAddLabelClick
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_proton_plus),
                contentDescription = NO_CONTENT_DESCRIPTION,
                modifier = Modifier
                    .testTag(LabelAsBottomSheetTestTags.AddLabelIcon)
                    .padding(ProtonDimens.DefaultSpacing),
                tint = ProtonTheme.colors.iconNorm
            )
            Text(
                modifier = Modifier
                    .testTag(LabelAsBottomSheetTestTags.AddLabelText)
                    .weight(1f)
                    .padding(vertical = ProtonDimens.DefaultSpacing),
                text = stringResource(id = R.string.label_title_create_label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyColumn {
            items(labelAsDataState.labelUiModelsWithSelectedState) { itemLabel ->
                ProtonRawListItem(
                    modifier = Modifier
                        .testTag(LabelAsBottomSheetTestTags.LabelItem)
                        .clickable { actions.onLabelAsSelected(itemLabel.labelUiModel.id.labelId) }
                        .height(ProtonDimens.ListItemHeight)
                ) {
                    Icon(
                        modifier = Modifier
                            .testTag(LabelAsBottomSheetTestTags.LabelIcon)
                            .semantics { tintColor = itemLabel.labelUiModel.iconTint }
                            .padding(horizontal = ProtonDimens.DefaultSpacing),
                        painter = painterResource(id = itemLabel.labelUiModel.icon),
                        contentDescription = NO_CONTENT_DESCRIPTION,
                        tint = itemLabel.labelUiModel.iconTint ?: ProtonTheme.colors.iconWeak
                    )
                    Text(
                        modifier = Modifier
                            .testTag(LabelAsBottomSheetTestTags.LabelNameText)
                            .weight(1f),
                        text = itemLabel.labelUiModel.text.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(
                        modifier = Modifier
                            .testTag(LabelAsBottomSheetTestTags.LabelSpacer)
                            .size(ProtonDimens.SmallSpacing)
                    )
                    TriStateCheckbox(
                        modifier = Modifier
                            .testTag(LabelAsBottomSheetTestTags.LabelSelectionCheckbox)
                            .padding(end = ProtonDimens.ExtraSmallSpacing),
                        state = when (itemLabel.selectedState) {
                            LabelSelectedState.Selected -> ToggleableState.On
                            LabelSelectedState.NotSelected -> ToggleableState.Off
                            LabelSelectedState.PartiallySelected -> ToggleableState.Indeterminate
                        },
                        onClick = { actions.onLabelAsSelected(itemLabel.labelUiModel.id.labelId) }
                    )
                }
            }
        }
    }
}

object LabelAsBottomSheetContent {

    data class Actions(
        val onAddLabelClick: () -> Unit,
        val onLabelAsSelected: (LabelId) -> Unit,
        val onDoneClick: (archiveSelected: Boolean, entryPoint: LabelAsBottomSheetEntryPoint) -> Unit
    )
}

@Preview(showBackground = true)
@Composable
fun LabelAsBottomSheetContentPreview() {
    ProtonTheme {
        LabelAsBottomSheetContent(
            labelAsDataState = LabelAsBottomSheetState.Data(
                labelUiModelsWithSelectedState = LabelUiModelWithSelectedStateSample.customLabelListWithSelection
                    .toImmutableList(),
                entryPoint = LabelAsBottomSheetEntryPoint.Conversation
            ),
            actions = LabelAsBottomSheetContent.Actions(
                onAddLabelClick = {},
                onLabelAsSelected = {},
                onDoneClick = { _, _ -> }
            )
        )
    }
}

object LabelAsBottomSheetTestTags {

    const val RootItem = "LabelAsBottomSheetRootItem"
    const val LabelAsText = "LabelAsText"
    const val DoneButton = "DoneButton"
    const val Divider = "LabelAsBottomSheetDivider"
    const val AlsoArchiveToggle = "AlsoArchiveToggle"
    const val AddLabelText = "AddLabelText"
    const val AddLabelIcon = "AddLabelIcon"
    const val AddLabelRow = "AddLabelRow"
    const val LabelItem = "LabelItem"
    const val LabelIcon = "LabelIcon"
    const val LabelNameText = "LabelNameText"
    const val LabelSpacer = "LabelSpacer"
    const val LabelSelectionCheckbox = "LabelSelectionCheckbox"
}
