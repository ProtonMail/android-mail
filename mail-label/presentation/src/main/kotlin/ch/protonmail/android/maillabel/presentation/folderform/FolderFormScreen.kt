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

package ch.protonmail.android.maillabel.presentation.folderform

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import ch.protonmail.android.maillabel.presentation.previewdata.FolderFormPreviewData.createFolderFormState
import ch.protonmail.android.maillabel.presentation.previewdata.FolderFormPreviewData.editFolderFormState
import ch.protonmail.android.maillabel.presentation.ui.ColorPicker
import ch.protonmail.android.maillabel.presentation.ui.FormDeleteButton
import ch.protonmail.android.maillabel.presentation.ui.FormInputField
import ch.protonmail.android.maillabel.presentation.upselling.FoldersUpsellingBottomSheet
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.ui.bottomsheet.UpsellingBottomSheet.DELAY_SHOWING
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.delay
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSettingsToggleItem
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.label.domain.entity.LabelId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FolderFormScreen(
    actions: FolderFormScreen.Actions,
    currentParentLabelId: State<String?>?,
    viewModel: FolderFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR) }
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var showBottomSheet by remember { mutableStateOf(false) }

    currentParentLabelId?.value?.let {
        // Initial value will always be null when initializing the view,
        //  so we use empty String to clear the current parent label id value
        val labelId =
            if (it.isEmpty()) null
            else LabelId(it)
        viewModel.submit(FolderFormViewAction.FolderParentChanged(labelId))
    }

    val customActions = actions.copy(
        onFolderNameChanged = {
            viewModel.submit(FolderFormViewAction.FolderNameChanged(it))
        },
        onFolderColorChanged = {
            viewModel.submit(FolderFormViewAction.FolderColorChanged(it))
        },
        onFolderNotificationsChanged = {
            viewModel.submit(FolderFormViewAction.FolderNotificationsChanged(it))
        },
        onSaveClick = {
            dismissKeyboard(context, view, keyboardController)
            viewModel.submit(FolderFormViewAction.OnSaveClick)
        },
        onDeleteClick = {
            dismissKeyboard(context, view, keyboardController)
            viewModel.submit(FolderFormViewAction.OnDeleteRequested)
        }
    )

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(FolderFormViewAction.HideUpselling)
    }

    if (state is FolderFormState.Data.Update) {
        DeleteDialog(
            state = state.confirmDeleteDialogState,
            confirm = { viewModel.submit(FolderFormViewAction.OnDeleteConfirmed) },
            dismiss = { viewModel.submit(FolderFormViewAction.OnDeleteCanceled) }
        )
    }

    if (state is FolderFormState.Data.Create) {
        ConsumableLaunchedEffect(effect = state.upsellingVisibility) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> {
                    bottomSheetState.hide()
                    showBottomSheet = false
                }

                BottomSheetVisibilityEffect.Show -> {
                    showBottomSheet = true
                    delay(DELAY_SHOWING)
                    focusManager.clearFocus()
                    bottomSheetState.show()
                }
            }
        }
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            if (showBottomSheet) {
                FoldersUpsellingBottomSheet(
                    actions = UpsellingScreen.Actions.Empty.copy(
                        onDismiss = { viewModel.submit(FolderFormViewAction.HideUpselling) },
                        onUpgrade = { message -> actions.showUpsellingSnackbar(message) },
                        onError = { message -> actions.showUpsellingErrorSnackbar(message) }
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                FolderFormTopBar(
                    state = state,
                    onCloseFolderFormClick = customActions.onBackClick,
                    onSaveFolderClick = customActions.onSaveClick
                )
            },
            content = { paddingValues ->
                when (state) {
                    is FolderFormState.Data -> {
                        FolderFormContent(
                            state = state,
                            actions = customActions,
                            modifier = Modifier.padding(paddingValues)
                        )

                        ConsumableTextEffect(effect = state.closeWithSuccess) { message ->
                            actions.exitWithSuccessMessage(message)
                        }

                        ConsumableTextEffect(effect = state.showErrorSnackbar) { message ->
                            snackbarHostState.showSnackbar(message = message, type = ProtonSnackbarType.ERROR)
                        }

                        if (state is FolderFormState.Data.Create) {
                            ConsumableTextEffect(effect = state.upsellingInProgress) { message ->
                                snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(message = message, type = ProtonSnackbarType.NORM)
                            }

                            ConsumableTextEffect(effect = state.showNormSnackbar) { message ->
                                snackbarHostState.showSnackbar(message = message, type = ProtonSnackbarType.NORM)
                            }
                        }
                    }

                    is FolderFormState.Loading -> {
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
            },
            snackbarHost = {
                DismissableSnackbarHost(
                    modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                    protonSnackbarHostState = snackbarHostState
                )
            }
        )
    }

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        customActions.onBackClick()
    }
}

@Composable
fun FolderFormContent(
    state: FolderFormState.Data,
    actions: FolderFormScreen.Actions,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = modifier.fillMaxSize()
    ) {
        val (deleteButton) = createRefs()

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(bottom = MailDimens.ScrollableFormBottomButtonSpacing)
        ) {
            FormInputField(
                initialValue = state.name,
                title = stringResource(R.string.label_name_title),
                hint = stringResource(R.string.add_a_folder_name_hint),
                onTextChange = {
                    actions.onFolderNameChanged(it)
                }
            )
            Divider()
            FolderFormParentFolderField(state, actions)
            val notificationsHintResId =
                if (state.notifications) R.string.switch_on
                else R.string.switch_off
            ProtonSettingsToggleItem(
                name = stringResource(id = R.string.folder_form_notifications),
                hint = stringResource(id = notificationsHintResId),
                value = state.notifications,
                onToggle = {
                    actions.onFolderNotificationsChanged(it)
                }
            )
            if (state.displayColorPicker) {
                Divider()
                ColorPicker(
                    colors = state.colorList,
                    selectedColor = state.color.getColorFromHexString(),
                    iconResId = R.drawable.ic_proton_folder_filled,
                    onColorClicked = {
                        actions.onFolderColorChanged(it)
                    }
                )
            }
        }
        if (state is FolderFormState.Data.Update) {
            FormDeleteButton(
                modifier = Modifier
                    .constrainAs(deleteButton) {
                        bottom.linkTo(parent.bottom, margin = ProtonDimens.DefaultSpacing)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(top = ProtonDimens.LargerSpacing),
                text = stringResource(id = R.string.folder_form_delete),
                onClick = actions.onDeleteClick
            )
        }
    }
}

@Composable
fun FolderFormParentFolderField(state: FolderFormState.Data, actions: FolderFormScreen.Actions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.folder_form_parent),
                role = Role.Button,
                onClick = {
                    actions.onFolderParentClick(
                        if (state is FolderFormState.Data.Update) state.labelId else null,
                        state.parent?.labelId
                    )
                }
            )
    ) {
        Text(
            text = stringResource(id = R.string.folder_form_parent),
            modifier = Modifier.padding(
                top = ProtonDimens.DefaultSpacing,
                start = ProtonDimens.DefaultSpacing
            ),
            style = ProtonTheme.typography.defaultNorm
        )
        Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
        Text(
            modifier = Modifier.padding(
                start = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.DefaultSpacing
            ),
            text = state.parent?.name ?: stringResource(id = R.string.folder_form_no_parent),
            color = ProtonTheme.colors.textHint,
            style = ProtonTheme.typography.defaultSmallWeak
        )
    }
    Divider()
}

@Composable
fun FolderFormTopBar(
    state: FolderFormState,
    onCloseFolderFormClick: () -> Unit,
    onSaveFolderClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            val title = when (state) {
                is FolderFormState.Data.Create -> stringResource(id = R.string.folder_form_create_folder)
                is FolderFormState.Data.Update -> stringResource(id = R.string.folder_form_edit_folder)
                is FolderFormState.Loading -> ""
            }
            Text(text = title)
        },
        navigationIcon = {
            IconButton(onClick = onCloseFolderFormClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            val displayCreateLoader = state is FolderFormState.Data.Create && state.displayCreateLoader
            if (displayCreateLoader) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = ProtonDimens.DefaultSpacing)
                        .size(MailDimens.ProgressDefaultSize),
                    strokeWidth = MailDimens.ProgressStrokeWidth
                )
            } else {
                ProtonTextButton(
                    onClick = onSaveFolderClick,
                    enabled = state.isSaveEnabled
                ) {
                    val textColor =
                        if (state.isSaveEnabled) ProtonTheme.colors.textAccent
                        else ProtonTheme.colors.interactionDisabled
                    Text(
                        text = stringResource(id = R.string.label_form_save),
                        color = textColor,
                        style = ProtonTheme.typography.defaultStrongNorm
                    )
                }
            }
        }
    )
}

object FolderFormScreen {

    const val FolderFormLabelIdKey = "folder_form_label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val exitWithSuccessMessage: (String) -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val onFolderNameChanged: (String) -> Unit,
        val onFolderColorChanged: (Color) -> Unit,
        val onFolderNotificationsChanged: (Boolean) -> Unit,
        val onFolderParentClick: (LabelId?, LabelId?) -> Unit,
        val onSaveClick: () -> Unit,
        val onDeleteClick: () -> Unit,
        val showUpsellingSnackbar: (String) -> Unit,
        val showUpsellingErrorSnackbar: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                exitWithSuccessMessage = {},
                exitWithErrorMessage = {},
                onFolderNameChanged = {},
                onFolderColorChanged = {},
                onFolderNotificationsChanged = {},
                onFolderParentClick = { _, _ -> },
                onSaveClick = {},
                onDeleteClick = {},
                showUpsellingSnackbar = {},
                showUpsellingErrorSnackbar = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateFolderFormScreenPreview() {
    FolderFormContent(
        state = createFolderFormState,
        actions = FolderFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditFolderFormScreenPreview() {
    FolderFormContent(
        state = editFolderFormState,
        actions = FolderFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateFolderFormTopBarPreview() {
    FolderFormTopBar(
        state = createFolderFormState,
        onCloseFolderFormClick = {},
        onSaveFolderClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditFolderFormTopBarPreview() {
    FolderFormTopBar(
        state = editFolderFormState,
        onCloseFolderFormClick = {},
        onSaveFolderClick = {}
    )
}
