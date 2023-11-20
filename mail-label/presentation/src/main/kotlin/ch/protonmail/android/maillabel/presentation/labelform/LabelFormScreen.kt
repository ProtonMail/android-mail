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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import ch.protonmail.android.maillabel.presentation.previewdata.LabelFormPreviewData.createLabelFormState
import ch.protonmail.android.maillabel.presentation.previewdata.LabelFormPreviewData.editLabelFormState
import ch.protonmail.android.maillabel.presentation.ui.ColorPicker
import ch.protonmail.android.maillabel.presentation.ui.FormDeleteButton
import ch.protonmail.android.maillabel.presentation.ui.FormInputField
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LabelFormScreen(actions: LabelFormScreen.Actions, viewModel: LabelFormViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)

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
            viewModel.submit(LabelFormViewAction.OnDeleteClick)
        }
    )

    val state = rememberAsState(
        flow = viewModel.state,
        initial = LabelFormState.Loading(Effect.empty())
    ).value

    Scaffold(
        topBar = {
            LabelFormTopBar(
                state = state,
                onCloseLabelFormClick = {
                    viewModel.submit(LabelFormViewAction.OnCloseLabelForm)
                },
                onSaveLabelClick = {
                    viewModel.submit(LabelFormViewAction.OnSaveClick)
                }
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
                        snackbarHostErrorState.showSnackbar(
                            message = labelAlreadyExistsMessage,
                            type = ProtonSnackbarType.ERROR
                        )
                    }
                    val labelLimitReachedMessage = stringResource(id = R.string.label_limit_reached_error)
                    ConsumableLaunchedEffect(effect = state.showLabelLimitReachedSnackbar) {
                        snackbarHostErrorState.showSnackbar(
                            message = labelLimitReachedMessage,
                            type = ProtonSnackbarType.ERROR
                        )
                    }
                    val saveLabelErrorMessage = stringResource(id = R.string.save_label_error)
                    ConsumableLaunchedEffect(effect = state.showSaveLabelErrorSnackbar) {
                        snackbarHostErrorState.showSnackbar(
                            message = saveLabelErrorMessage,
                            type = ProtonSnackbarType.ERROR
                        )
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
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                hostState = snackbarHostErrorState
            )
        }
    )

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
    Column(
        modifier = modifier
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
        if (state is LabelFormState.Data.Update) {
            FormDeleteButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
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
    )
}

object LabelFormScreen {

    const val LabelIdKey = "label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val showLabelSavedSnackbar: () -> Unit,
        val showLabelDeletedSnackbar: () -> Unit,
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
private fun EditLabelFormTopBarPreview() {
    LabelFormTopBar(
        state = editLabelFormState,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}
