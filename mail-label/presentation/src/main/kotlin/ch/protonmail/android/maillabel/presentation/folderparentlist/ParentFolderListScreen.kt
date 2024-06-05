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

package ch.protonmail.android.maillabel.presentation.folderparentlist

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.model.ParentFolderUiModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.label.domain.entity.LabelId

@Composable
fun ParentFolderListScreen(
    actions: ParentFolderListScreen.Actions,
    viewModel: ParentFolderListViewModel = hiltViewModel()
) {
    val state = rememberAsState(flow = viewModel.state, initial = viewModel.initialState).value

    Scaffold(
        topBar = {
            ParentFolderListTopBar(
                modifier = Modifier,
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            when (state) {
                is ParentFolderListState.ListLoaded.Data -> {
                    ParentFolderListScreenContent(
                        modifier = Modifier.padding(
                            PaddingValues(
                                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                                top = paddingValues.calculateTopPadding() + ProtonDimens.SmallSpacing,
                                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                                bottom = paddingValues.calculateBottomPadding()
                            )
                        ),
                        state = state,
                        actions = actions
                    )
                }

                is ParentFolderListState.ListLoaded.Empty -> {
                    EmptyParentFolderListScreen(
                        modifier = Modifier.padding(paddingValues),
                        actions = actions,
                        state = state
                    )
                }

                is ParentFolderListState.Loading -> {
                    ProtonCenteredProgress(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    )

                    ConsumableTextEffect(effect = state.errorLoading) { message ->
                        actions.exitWithErrorMessage(message)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParentFolderListScreenContent(
    modifier: Modifier = Modifier,
    state: ParentFolderListState.ListLoaded.Data,
    actions: ParentFolderListScreen.Actions
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(state.folders) { index, parentFolderUiModel ->
            if (index == 0) NoneListItem(actions = actions, state = state)
            if (parentFolderUiModel.displayDivider) Divider()
            if (parentFolderUiModel.isEnabled) {
                ClickableParentFolderItem(actions = actions, parentFolderUiModel = parentFolderUiModel)
            } else {
                DisabledParentFolderItem(parentFolderUiModel = parentFolderUiModel)
            }
        }
    }
}

@Composable
fun ClickableParentFolderItem(
    modifier: Modifier = Modifier,
    actions: ParentFolderListScreen.Actions,
    parentFolderUiModel: ParentFolderUiModel
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = { actions.onFolderSelected(parentFolderUiModel.folder.id) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(
                start = ProtonDimens.DefaultSpacing.times(parentFolderUiModel.folder.level.plus(1)),
                end = ProtonDimens.DefaultSpacing
            ),
            painter = painterResource(id = parentFolderUiModel.folder.icon),
            tint = parentFolderUiModel.folder.displayColor ?: ProtonTheme.colors.iconNorm,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            text = parentFolderUiModel.folder.name,
            modifier = Modifier
                .padding(
                    start = ProtonDimens.ExtraSmallSpacing,
                    top = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.DefaultSpacing,
                    bottom = ProtonDimens.DefaultSpacing
                )
                .weight(1f),
            style = ProtonTheme.typography.defaultNorm
        )
        if (parentFolderUiModel.isSelected) {
            Icon(
                modifier = Modifier.padding(
                    end = ProtonDimens.DefaultSpacing
                ),
                painter = painterResource(id = R.drawable.ic_proton_checkmark),
                tint = ProtonTheme.colors.iconAccent,
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
fun DisabledParentFolderItem(modifier: Modifier = Modifier, parentFolderUiModel: ParentFolderUiModel) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(
                start = ProtonDimens.DefaultSpacing.times(parentFolderUiModel.folder.level.plus(1)),
                end = ProtonDimens.DefaultSpacing
            ),
            painter = painterResource(id = parentFolderUiModel.folder.icon),
            tint = parentFolderUiModel.folder.displayColor?.copy(alpha = 0.5f) ?: ProtonTheme.colors.iconDisabled,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            text = parentFolderUiModel.folder.name,
            modifier = Modifier
                .padding(
                    start = ProtonDimens.ExtraSmallSpacing,
                    top = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.DefaultSpacing,
                    bottom = ProtonDimens.DefaultSpacing
                )
                .weight(1f),
            style = ProtonTheme.typography.defaultNorm,
            color = ProtonTheme.colors.textDisabled
        )
    }
}

@Composable
fun NoneListItem(
    modifier: Modifier = Modifier,
    actions: ParentFolderListScreen.Actions,
    state: ParentFolderListState.ListLoaded
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = actions.onNoneClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.parent_folder_none),
            modifier = Modifier
                .padding(ProtonDimens.DefaultSpacing)
                .weight(1f),
            style = ProtonTheme.typography.defaultNorm
        )
        if (state.parentLabelId == null) {
            Icon(
                modifier = Modifier.padding(end = ProtonDimens.DefaultSpacing),
                painter = painterResource(id = R.drawable.ic_proton_checkmark),
                tint = ProtonTheme.colors.iconAccent,
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
    Divider()
}

@Composable
fun EmptyParentFolderListScreen(
    modifier: Modifier = Modifier,
    actions: ParentFolderListScreen.Actions,
    state: ParentFolderListState.ListLoaded.Empty
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NoneListItem(actions = actions, state = state)
    }
}

@Composable
fun ParentFolderListTopBar(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    ProtonTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(text = stringResource(id = R.string.label_title_parent_folder))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        }
    )
}

object ParentFolderListScreen {

    const val ParentFolderListLabelIdKey = "parent_folder_list_label_id"
    const val ParentFolderListParentLabelIdKey = "parent_folder_list_parent_label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val onFolderSelected: (LabelId) -> Unit,
        val onNoneClick: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onFolderSelected = {},
                onNoneClick = {},
                exitWithErrorMessage = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyParentFolderListScreenPreview() {
    EmptyParentFolderListScreen(
        actions = ParentFolderListScreen.Actions.Empty,
        state = ParentFolderListState.ListLoaded.Empty(
            labelId = LabelId("labelId"),
            parentLabelId = null
        )
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ParentFolderListTopBarPreview() {
    ParentFolderListTopBar(
        onBackClick = {}
    )
}
