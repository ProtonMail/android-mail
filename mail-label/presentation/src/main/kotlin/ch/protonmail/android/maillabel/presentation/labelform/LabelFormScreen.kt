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

package ch.protonmail.android.maillabel.presentation.labelform

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import ch.protonmail.android.maillabel.presentation.previewdata.LabelFormPreviewData.createLabelFormState
import ch.protonmail.android.maillabel.presentation.previewdata.LabelFormPreviewData.editLabelFormState
import ch.protonmail.android.maillabel.presentation.ui.ColorPicker
import ch.protonmail.android.maillabel.presentation.ui.FormDeleteButton
import ch.protonmail.android.maillabel.presentation.ui.FormInputField
import ch.protonmail.android.maillabel.presentation.upselling.LabelsUpsellingBottomSheet
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.ui.bottomsheet.UpsellingBottomSheet.DELAY_SHOWING
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.delay
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LabelFormScreen(actions: LabelFormScreen.Actions, viewModel: LabelFormViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR) }

    val customActions = actions.copy(
        onLabelNameChanged = {
            viewModel.submit(LabelFormViewAction.LabelNameChanged(it))
        },
        onLabelColorChanged = {
            viewModel.submit(LabelFormViewAction.LabelColorChanged(it))
        },
        onSaveClick = {
            dismissKeyboard(context, view, keyboardController)
            viewModel.submit(LabelFormViewAction.OnSaveClick)
        },
        onDeleteClick = {
            dismissKeyboard(context, view, keyboardController)
            viewModel.submit(LabelFormViewAction.OnDeleteRequested)
        }
    )

    val state = viewModel.state.collectAsStateWithLifecycle().value
    var showBottomSheet by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(LabelFormViewAction.HideUpselling)
    }

    if (state is LabelFormState.Data.Update) {
        DeleteDialog(
            state = state.confirmDeleteDialogState,
            confirm = { viewModel.submit(LabelFormViewAction.OnDeleteConfirmed) },
            dismiss = { viewModel.submit(LabelFormViewAction.OnDeleteCanceled) }
        )
    }

    if (state is LabelFormState.Data.Create) {
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
                LabelsUpsellingBottomSheet(
                    actions = UpsellingScreen.Actions.Empty.copy(
                        onDismiss = { viewModel.submit(LabelFormViewAction.HideUpselling) },
                        onUpgrade = { message -> actions.showUpsellingSnackbar(message) },
                        onError = { message -> actions.showUpsellingErrorSnackbar(message) }
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                LabelFormTopBar(
                    state = state,
                    onCloseLabelFormClick = customActions.onBackClick,
                    onSaveLabelClick = customActions.onSaveClick
                )
            },
            content = { paddingValues ->
                when (state) {
                    is LabelFormState.Data -> {
                        LabelFormContent(
                            state = state,
                            actions = customActions,
                            modifier = Modifier.padding(paddingValues)
                        )

                        ConsumableLaunchedEffect(effect = state.closeWithSave) {
                            customActions.onBackClick()
                            actions.showLabelSavedSnackbar()
                        }
                        val labelAlreadyExistsMessage = stringResource(id = R.string.label_already_exists)
                        ConsumableLaunchedEffect(effect = state.showLabelAlreadyExistsSnackbar) {
                            snackbarHostState.showSnackbar(
                                message = labelAlreadyExistsMessage,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                        val labelLimitReachedMessage = stringResource(id = R.string.label_limit_reached_error)
                        ConsumableLaunchedEffect(effect = state.showLabelLimitReachedSnackbar) {
                            snackbarHostState.showSnackbar(
                                message = labelLimitReachedMessage,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                        val saveLabelErrorMessage = stringResource(id = R.string.save_label_error)
                        ConsumableLaunchedEffect(effect = state.showSaveLabelErrorSnackbar) {
                            snackbarHostState.showSnackbar(
                                message = saveLabelErrorMessage,
                                type = ProtonSnackbarType.ERROR
                            )
                        }

                        if (state is LabelFormState.Data.Create) {
                            ConsumableTextEffect(effect = state.upsellingInProgress) { message ->
                                snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    type = ProtonSnackbarType.NORM
                                )
                            }
                        }

                        if (state is LabelFormState.Data.Update) {
                            ConsumableLaunchedEffect(effect = state.closeWithDelete) {
                                customActions.onBackClick()
                                actions.showLabelDeletedSnackbar()
                            }
                        }
                    }

                    is LabelFormState.Loading -> {
                        ProtonCenteredProgress(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        )
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
fun LabelFormContent(
    state: LabelFormState.Data,
    actions: LabelFormScreen.Actions,
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
                hint = stringResource(R.string.add_a_label_name_hint),
                onTextChange = {
                    actions.onLabelNameChanged(it)
                }
            )
            Divider()
            ColorPicker(
                colors = state.colorList,
                selectedColor = state.color.getColorFromHexString(),
                onColorClicked = {
                    actions.onLabelColorChanged(it)
                }
            )
        }
        if (state is LabelFormState.Data.Update) {
            FormDeleteButton(
                modifier = Modifier
                    .constrainAs(deleteButton) {
                        bottom.linkTo(parent.bottom, margin = ProtonDimens.DefaultSpacing)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(top = ProtonDimens.LargerSpacing),
                text = stringResource(id = R.string.label_form_delete),
                onClick = actions.onDeleteClick
            )
        }
    }
}

@Composable
fun LabelFormTopBar(
    state: LabelFormState,
    onCloseLabelFormClick: () -> Unit,
    onSaveLabelClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            val title = when (state) {
                is LabelFormState.Data.Create -> stringResource(id = R.string.label_form_create_label)
                is LabelFormState.Data.Update -> stringResource(id = R.string.label_form_edit_label)
                is LabelFormState.Loading -> ""
            }
            Text(text = title)
        },
        navigationIcon = {
            IconButton(onClick = onCloseLabelFormClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            val displayCreateLoader = state is LabelFormState.Data.Create && state.displayCreateLoader
            if (displayCreateLoader) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = ProtonDimens.DefaultSpacing)
                        .size(MailDimens.ProgressDefaultSize),
                    strokeWidth = MailDimens.ProgressStrokeWidth
                )
            } else {
                ProtonTextButton(
                    onClick = onSaveLabelClick,
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

object LabelFormScreen {

    const val LabelFormLabelIdKey = "label_form_label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val showLabelSavedSnackbar: () -> Unit,
        val showLabelDeletedSnackbar: () -> Unit,
        val showUpsellingSnackbar: (String) -> Unit,
        val showUpsellingErrorSnackbar: (String) -> Unit,
        val onLabelNameChanged: (String) -> Unit,
        val onLabelColorChanged: (Color) -> Unit,
        val onSaveClick: () -> Unit,
        val onDeleteClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                showLabelSavedSnackbar = {},
                showLabelDeletedSnackbar = {},
                showUpsellingSnackbar = {},
                showUpsellingErrorSnackbar = {},
                onLabelNameChanged = {},
                onLabelColorChanged = {},
                onSaveClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateLabelFormScreenPreview() {
    LabelFormContent(
        state = createLabelFormState,
        actions = LabelFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormScreenPreview() {
    LabelFormContent(
        state = editLabelFormState,
        actions = LabelFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateLabelFormTopBarPreview() {
    LabelFormTopBar(
        state = createLabelFormState,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateLabelFormTopBarLoaderPreview() {
    LabelFormTopBar(
        state = createLabelFormState.copy(displayCreateLoader = true),
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormTopBarPreview() {
    LabelFormTopBar(
        state = editLabelFormState,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}
