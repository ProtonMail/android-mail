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

package ch.protonmail.android.maillabel.presentation.folderlist

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.previewdata.FolderListPreviewData.folderSampleData
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.ProtonSettingsToggleItem
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultUnspecified
import me.proton.core.compose.theme.interactionNorm
import me.proton.core.label.domain.entity.LabelId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FolderListScreen(actions: FolderListScreen.Actions, viewModel: FolderListViewModel = hiltViewModel()) {
    val state = rememberAsState(
        flow = viewModel.state,
        initial = viewModel.initialState
    ).value
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) { onDispose { viewModel.submit(FolderListViewAction.OnDismissSettings) } }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(FolderListViewAction.OnDismissSettings)
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (state) {
                is FolderListState.Loading -> {}
                is FolderListState.ListLoaded -> {
                    FolderSettingsScreen(
                        state = state,
                        onChangeUseFolderColor = { useFolderColor ->
                            viewModel.submit(
                                FolderListViewAction.OnChangeUseFolderColor(
                                    useFolderColor = useFolderColor
                                )
                            )
                        },
                        onChangeInheritParentFolderColor = { inheritParentFolderColor ->
                            viewModel.submit(
                                FolderListViewAction.OnChangeInheritParentFolderColor(
                                    inheritParentFolderColor = inheritParentFolderColor
                                )
                            )
                        },
                        onDoneClick = { viewModel.submit(FolderListViewAction.OnDismissSettings) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                FolderListTopBar(
                    actions = actions,
                    onAddFolderClick = { viewModel.submit(FolderListViewAction.OnAddFolderClick) },
                    onFolderSettingsClick = { viewModel.submit(FolderListViewAction.OnOpenSettingsClick) },
                    isAddFolderButtonVisible = state is FolderListState.ListLoaded.Data,
                    isSettingsButtonVisible = state is FolderListState.ListLoaded
                )
            },
            content = { paddingValues ->
                when (state) {
                    is FolderListState.ListLoaded.Data -> {
                        FolderListScreenContent(
                            state = state,
                            actions = actions,
                            paddingValues = paddingValues
                        )

                        ConsumableLaunchedEffect(effect = state.openFolderForm) {
                            actions.onAddFolderClick()
                        }
                        ConsumableLaunchedEffect(effect = state.bottomSheetVisibilityEffect) {
                            when (it) {
                                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
                            }
                        }
                    }

                    is FolderListState.ListLoaded.Empty -> {
                        EmptyFolderListScreen(
                            onAddFolderClick = { viewModel.submit(FolderListViewAction.OnAddFolderClick) },
                            paddingValues = paddingValues
                        )

                        ConsumableLaunchedEffect(effect = state.openFolderForm) {
                            actions.onAddFolderClick()
                        }
                        ConsumableLaunchedEffect(effect = state.bottomSheetVisibilityEffect) {
                            when (it) {
                                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
                            }
                        }
                    }

                    is FolderListState.Loading -> {
                        ProtonCenteredProgress(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        )

                        ConsumableLaunchedEffect(effect = state.errorLoading) {
                            actions.onBackClick()
                            actions.showFolderListErrorLoadingSnackbar()
                        }
                    }
                }
            }
        )
    }
}

@SuppressWarnings("UseComposableActions")
@Composable
fun FolderSettingsScreen(
    state: FolderListState.ListLoaded,
    onChangeUseFolderColor: (Boolean) -> Unit,
    onChangeInheritParentFolderColor: (Boolean) -> Unit,
    onDoneClick: () -> Unit
) {
    var useFolderColorState by remember { mutableStateOf(state.useFolderColor) }
    var inheritParentFolderColorState by remember { mutableStateOf(state.inheritParentFolderColor) }

    Column {
        Row(
            modifier = Modifier
                .padding(ProtonDimens.DefaultSpacing)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.folder_settings_title),
                style = ProtonTheme.typography.defaultNorm
            )
            Text(
                modifier = Modifier
                    .clickable { onDoneClick() },
                text = stringResource(id = R.string.folder_settings_done_button),
                style = ProtonTheme.typography.defaultUnspecified,
                color = ProtonTheme.colors.interactionNorm()
            )
        }
        Divider()
        val useFolderColorHintResId =
            if (useFolderColorState) R.string.folder_settings_switch_on
            else R.string.folder_settings_switch_off
        ProtonSettingsToggleItem(
            name = stringResource(id = R.string.folder_settings_folder_colors),
            hint = stringResource(id = useFolderColorHintResId),
            value = useFolderColorState,
            onToggle = {
                useFolderColorState = !useFolderColorState
                onChangeUseFolderColor(useFolderColorState)
            }
        )
        AnimatedVisibility(visible = useFolderColorState) {
            Divider()
            val inheritParentFolderColorHintResId =
                if (inheritParentFolderColorState) R.string.folder_settings_switch_on
                else R.string.folder_settings_switch_off
            ProtonSettingsToggleItem(
                name = stringResource(id = R.string.folder_settings_parent_color),
                hint = stringResource(id = inheritParentFolderColorHintResId),
                value = inheritParentFolderColorState,
                onToggle = {
                    inheritParentFolderColorState = !inheritParentFolderColorState
                    onChangeInheritParentFolderColor(inheritParentFolderColorState)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListScreenContent(
    state: FolderListState.ListLoaded.Data,
    actions: FolderListScreen.Actions,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .padding(
                PaddingValues(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    top = paddingValues.calculateTopPadding() + ProtonDimens.SmallSpacing,
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding()
                )
            )
            .fillMaxSize()
    ) {
        itemsIndexed(state.folders) { index, folder ->
            if (index != 0 && folder.parent == null) {
                Divider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .clickable(
                        onClickLabel = stringResource(R.string.folder_list_item_content_description),
                        role = Role.Button,
                        onClick = {
                            actions.onFolderSelected(folder.id.labelId)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconResId =
                    if (folder.children.isNotEmpty()) {
                        if (state.useFolderColor) R.drawable.ic_proton_folders_filled
                        else R.drawable.ic_proton_folders
                    } else {
                        if (state.useFolderColor) R.drawable.ic_proton_folder_filled
                        else R.drawable.ic_proton_folder
                    }
                val iconTint =
                    if (state.useFolderColor &&
                        state.inheritParentFolderColor
                    ) {
                        var parentFolder = folder.parent
                        while (parentFolder?.parent != null) {
                            parentFolder = parentFolder.parent
                        }
                        Color(parentFolder?.color ?: folder.color)
                    } else if (state.useFolderColor) {
                        Color(folder.color)
                    } else ProtonTheme.colors.iconNorm

                Icon(
                    modifier = Modifier.padding(
                        start = ProtonDimens.DefaultSpacing.times(folder.level.plus(1)),
                        end = ProtonDimens.DefaultSpacing
                    ),
                    painter = painterResource(id = iconResId),
                    tint = iconTint,
                    contentDescription = stringResource(R.string.add_folder_content_description)
                )
                Text(
                    text = folder.text,
                    modifier = Modifier.padding(
                        start = ProtonDimens.ExtraSmallSpacing,
                        top = ProtonDimens.DefaultSpacing,
                        end = ProtonDimens.DefaultSpacing,
                        bottom = ProtonDimens.DefaultSpacing
                    ),
                    style = ProtonTheme.typography.defaultNorm
                )
            }
        }
    }
}

@Composable
fun EmptyFolderListScreen(onAddFolderClick: () -> Unit, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .padding(start = ProtonDimens.ExtraSmallSpacing)
                .background(
                    color = ProtonTheme.colors.backgroundSecondary,
                    shape = RoundedCornerShape(MailDimens.IconWeakRoundBackgroundRadius)
                )
                .padding(ProtonDimens.SmallSpacing),
            painter = painterResource(id = R.drawable.ic_proton_folder_plus),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            stringResource(R.string.folder_list_no_folders_found),
            Modifier.padding(
                start = ProtonDimens.LargeSpacing,
                top = ProtonDimens.MediumSpacing,
                end = ProtonDimens.LargeSpacing
            ),
            style = ProtonTheme.typography.defaultStrongNorm
        )
        Text(
            stringResource(R.string.folder_list_create_folder_placeholder_description),
            Modifier.padding(
                start = ProtonDimens.LargeSpacing,
                top = MailDimens.TinySpacing,
                end = ProtonDimens.LargeSpacing
            ),
            style = ProtonTheme.typography.defaultSmallWeak
        )
        ProtonSecondaryButton(
            modifier = Modifier.padding(top = ProtonDimens.LargeSpacing),
            onClick = onAddFolderClick
        ) {
            Text(
                text = stringResource(R.string.label_title_add_folder),
                Modifier.padding(
                    horizontal = ProtonDimens.SmallSpacing
                ),
                style = ProtonTheme.typography.captionNorm
            )
        }
    }
}

@Composable
fun FolderListTopBar(
    actions: FolderListScreen.Actions,
    onAddFolderClick: () -> Unit,
    onFolderSettingsClick: () -> Unit,
    isAddFolderButtonVisible: Boolean,
    isSettingsButtonVisible: Boolean
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(text = stringResource(id = R.string.label_title_folders))
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            if (isAddFolderButtonVisible) {
                IconButton(onClick = onAddFolderClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_plus),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.add_folder_content_description)
                    )
                }
            }
            if (isSettingsButtonVisible) {
                IconButton(onClick = onFolderSettingsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_cog_wheel),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.folder_settings_content_description)
                    )
                }
            }
        }
    )
}

object FolderListScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onFolderSelected: (LabelId) -> Unit,
        val onAddFolderClick: () -> Unit,
        val showFolderListErrorLoadingSnackbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onFolderSelected = {},
                onAddFolderClick = {},
                showFolderListErrorLoadingSnackbar = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun FolderListScreenPreview() {
    FolderListScreenContent(
        state = FolderListState.ListLoaded.Data(
            folders = listOf(
                folderSampleData,
                folderSampleData,
                folderSampleData
            ).toMailLabelCustom(),
            useFolderColor = true,
            inheritParentFolderColor = true
        ),
        actions = FolderListScreen.Actions.Empty,
        paddingValues = PaddingValues()
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyFolderListScreenPreview() {
    EmptyFolderListScreen(
        onAddFolderClick = {},
        paddingValues = PaddingValues()
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun FolderSettingsScreenPreview() {
    FolderSettingsScreen(
        state = FolderListState.ListLoaded.Empty(
            bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show),
            useFolderColor = true,
            inheritParentFolderColor = true
        ),
        onChangeUseFolderColor = {},
        onChangeInheritParentFolderColor = {},
        onDoneClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun FolderListTopBarPreview() {
    FolderListTopBar(
        actions = FolderListScreen.Actions.Empty,
        onAddFolderClick = {},
        onFolderSettingsClick = {},
        isAddFolderButtonVisible = true,
        isSettingsButtonVisible = true
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyFolderListTopBarPreview() {
    FolderListTopBar(
        actions = FolderListScreen.Actions.Empty,
        onAddFolderClick = {},
        onFolderSettingsClick = {},
        isAddFolderButtonVisible = false,
        isSettingsButtonVisible = true
    )
}
