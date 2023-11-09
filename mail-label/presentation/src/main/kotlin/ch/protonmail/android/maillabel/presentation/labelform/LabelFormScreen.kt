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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import ch.protonmail.android.maillabel.presentation.ui.ColorPicker
import ch.protonmail.android.maillabel.presentation.ui.FormInputField
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.component.protonOutlinedButtonColors
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultStrongNorm
import okhttp3.internal.toHexString

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LabelFormScreen(actions: LabelFormScreen.Actions, viewModel: LabelFormViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = LabelFormState.Loading
        ).value
    ) {
        is LabelFormState.Create -> {
            CreateLabelFormContent(
                name = state.name,
                color = state.color,
                isSaveEnabled = state.isSaveEnabled,
                actions = customActions,
                onCloseLabelFormClick = {
                    viewModel.submit(LabelFormViewAction.OnCloseLabelForm)
                },
                onSaveLabelClick = {
                    viewModel.submit(LabelFormViewAction.OnSaveClick)
                }
            )

            ConsumableLaunchedEffect(effect = state.close) {
                dismissKeyboard(context, view, keyboardController)
                customActions.onBackClick()
            }
            ConsumableLaunchedEffect(effect = state.closeWithSave) {
                customActions.onBackClick()
                actions.showLabelSavedSnackbar()
            }
        }
        is LabelFormState.Update -> {
            EditLabelFormContent(
                name = state.name,
                color = state.color,
                isSaveEnabled = state.isSaveEnabled,
                actions = customActions,
                onCloseLabelFormClick = {
                    viewModel.submit(LabelFormViewAction.OnCloseLabelForm)
                },
                onSaveLabelClick = {
                    viewModel.submit(LabelFormViewAction.OnSaveClick)
                }
            )

            ConsumableLaunchedEffect(effect = state.close) {
                dismissKeyboard(context, view, keyboardController)
                customActions.onBackClick()
            }
            ConsumableLaunchedEffect(effect = state.closeWithSave) {
                customActions.onBackClick()
                actions.showLabelSavedSnackbar()
            }
            ConsumableLaunchedEffect(effect = state.closeWithDelete) {
                customActions.onBackClick()
                actions.showLabelDeletedSnackbar()
            }
        }
        LabelFormState.Loading -> {
            // Loading do nothing
        }
    }
}

@Composable
fun CreateLabelFormContent(
    name: String,
    color: String,
    isSaveEnabled: Boolean,
    actions: LabelFormScreen.Actions,
    onCloseLabelFormClick: () -> Unit,
    onSaveLabelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            LabelFormTopBar(
                title = stringResource(id = R.string.label_form_create_label),
                isSaveEnabled = isSaveEnabled,
                onCloseLabelFormClick = onCloseLabelFormClick,
                onSaveLabelClick = onSaveLabelClick
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                FormInputField(
                    initialValue = name,
                    title = stringResource(R.string.label_name_title),
                    hint = stringResource(R.string.add_a_label_name_hint),
                    onTextChange = {
                        actions.onLabelNameChanged(it)
                    }
                )
                Divider()
                ColorPicker(
                    selectedColor = color.getColorFromHexString(),
                    onColorClicked = {
                        actions.onLabelColorChanged(it)
                    }
                )
            }
        }
    )
}

@Composable
fun EditLabelFormContent(
    name: String,
    color: String,
    isSaveEnabled: Boolean,
    actions: LabelFormScreen.Actions,
    onCloseLabelFormClick: () -> Unit,
    onSaveLabelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            LabelFormTopBar(
                title = stringResource(id = R.string.label_form_edit_label),
                isSaveEnabled = isSaveEnabled,
                onCloseLabelFormClick = onCloseLabelFormClick,
                onSaveLabelClick = onSaveLabelClick
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                FormInputField(
                    initialValue = name,
                    title = stringResource(R.string.label_name_title),
                    hint = stringResource(R.string.add_a_label_name_hint),
                    onTextChange = {
                        actions.onLabelNameChanged(it)
                    }
                )
                Divider()
                ColorPicker(
                    selectedColor = color.getColorFromHexString(),
                    onColorClicked = {
                        actions.onLabelColorChanged(it)
                    }
                )
                ProtonButton(
                    onClick = actions.onDeleteClick,
                    modifier = Modifier
                        .heightIn(min = ButtonDefaults.MinHeight)
                        .padding(top = ProtonDimens.LargerSpacing)
                        .align(Alignment.CenterHorizontally),
                    elevation = null,
                    shape = ProtonTheme.shapes.medium,
                    border = BorderStroke(
                        ButtonDefaults.OutlinedBorderSize,
                        ProtonTheme.colors.notificationError
                    ),
                    colors = ButtonDefaults.protonOutlinedButtonColors(
                        contentColor = ProtonTheme.colors.notificationError
                    ),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(
                        modifier = Modifier.padding(
                            start = ProtonDimens.MediumSpacing,
                            top = ProtonDimens.ExtraSmallSpacing,
                            end = ProtonDimens.MediumSpacing,
                            bottom = ProtonDimens.ExtraSmallSpacing
                        ),
                        text = stringResource(id = R.string.label_form_delete),
                        style = ProtonTheme.typography.defaultNorm,
                        color = ProtonTheme.colors.notificationError
                    )
                }
            }
        }
    )
}

@Composable
fun LabelFormTopBar(
    title: String,
    isSaveEnabled: Boolean,
    onCloseLabelFormClick: () -> Unit,
    onSaveLabelClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
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
                enabled = isSaveEnabled
            ) {
                Text(
                    text = stringResource(id = R.string.label_form_save),
                    color = if (isSaveEnabled) ProtonTheme.colors.textAccent
                    else ProtonTheme.colors.interactionDisabled,
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
    CreateLabelFormContent(
        name = "Label name",
        color = colorResource(id = R.color.chambray).toArgb().toHexString(),
        isSaveEnabled = true,
        actions = LabelFormScreen.Actions.Empty,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormScreenPreview() {
    EditLabelFormContent(
        name = "Label name",
        color = colorResource(id = R.color.chambray).toArgb().toHexString(),
        isSaveEnabled = true,
        actions = LabelFormScreen.Actions.Empty,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateLabelFormTopBarPreview() {
    LabelFormTopBar(
        title = stringResource(id = R.string.label_form_create_label),
        isSaveEnabled = false,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormTopBarPreview() {
    LabelFormTopBar(
        title = stringResource(id = R.string.label_form_edit_label),
        isSaveEnabled = true,
        onCloseLabelFormClick = {},
        onSaveLabelClick = {}
    )
}
