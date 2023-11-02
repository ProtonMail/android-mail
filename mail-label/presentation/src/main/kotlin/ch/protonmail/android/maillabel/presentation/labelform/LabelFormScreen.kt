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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import okhttp3.internal.toHexString

@Composable
fun LabelFormScreen(actions: LabelFormScreen.Actions, viewModel: LabelFormViewModel = hiltViewModel()) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = LabelFormState.Loading
        ).value
    ) {
        is LabelFormState.CreateLabel -> {
            CreateLabelFormScreen(state = state, actions = actions.buildActions(viewModel))
        }
        is LabelFormState.EditLabel -> {
            EditLabelFormScreen(state = state, actions = actions.buildActions(viewModel))
        }
        is LabelFormState.Loading -> { }
    }
}

private fun LabelFormScreen.Actions.buildActions(viewModel: LabelFormViewModel): LabelFormScreen.Actions {
    return this.copy(
        onLabelNameChanged = {
            viewModel.submit(LabelFormAction.LabelNameChanged(it))
        },
        onLabelColorChanged = {
            viewModel.submit(LabelFormAction.LabelColorChanged(it))
        },
        onSaveClick = {
            viewModel.submit(LabelFormAction.OnSaveClick)
        },
        onDeleteClick = {
            viewModel.submit(LabelFormAction.OnDeleteClick)
        }
    )
}

@Composable
fun CreateLabelFormScreen(state: LabelFormState.CreateLabel, actions: LabelFormScreen.Actions) {
    Scaffold(
        topBar = {
            LabelFormTopBar(
                actions,
                title = stringResource(id = R.string.label_form_create_label)
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                FormInputField(
                    initialValue = state.newLabel.name,
                    title = stringResource(R.string.label_name_title),
                    hint = stringResource(R.string.add_a_label_name_hint),
                    onTextChange = {
                        actions.onLabelNameChanged(it)
                    }
                )
                ColorPicker(
                    selectedColor = state.newLabel.color.getColorFromHexString(),
                    onColorClicked = {
                        actions.onLabelColorChanged(it)
                    }
                )
            }
        }
    )
}

@Composable
fun EditLabelFormScreen(state: LabelFormState.EditLabel, actions: LabelFormScreen.Actions) {
    Scaffold(
        topBar = {
            LabelFormTopBar(
                actions,
                title = stringResource(id = R.string.label_form_edit_label)
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                FormInputField(
                    initialValue = state.label.name,
                    title = stringResource(R.string.label_name_title),
                    hint = stringResource(R.string.add_a_label_name_hint),
                    onTextChange = {
                        actions.onLabelNameChanged(it)
                    }
                )
                ColorPicker(
                    selectedColor = state.label.color.getColorFromHexString(),
                    onColorClicked = {
                        actions.onLabelColorChanged(it)
                    }
                )
                ProtonButton(
                    onClick = actions.onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = ButtonDefaults.MinHeight)
                        .padding(top = ProtonDimens.LargerSpacing),
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
fun LabelFormTopBar(actions: LabelFormScreen.Actions, title: String) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = title,
                style = ProtonTheme.typography.defaultStrongNorm
            )
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            ProtonTextButton(onClick = actions.onSaveClick) {
                Text(
                    text = stringResource(id = R.string.label_form_save),
                    color = ProtonTheme.colors.textAccent,
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
        val onLabelNameChanged: (String) -> Unit,
        val onLabelColorChanged: (Color) -> Unit,
        val onSaveClick: () -> Unit,
        val onDeleteClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
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
    CreateLabelFormScreen(
        state = LabelFormState.CreateLabel(buildSampleNewLabel()),
        actions = LabelFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormScreenPreview() {
    EditLabelFormScreen(
        state = LabelFormState.EditLabel(
            buildSampleLabel(
                name = "Label name",
                color = colorResource(id = R.color.chambray).toArgb().toHexString()
            )
        ),
        actions = LabelFormScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateLabelFormTopBarPreview() {
    LabelFormTopBar(
        actions = LabelFormScreen.Actions.Empty,
        title = stringResource(id = R.string.label_form_create_label)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EditLabelFormTopBarPreview() {
    LabelFormTopBar(
        actions = LabelFormScreen.Actions.Empty,
        title = stringResource(id = R.string.label_form_edit_label)
    )
}

private fun buildSampleLabel(name: String, color: String): Label {
    return Label(
        userId = UserId("userId"),
        labelId = LabelId("labelId"),
        parentId = null,
        name = name,
        type = LabelType.MessageLabel,
        path = "path",
        color = color,
        order = 0,
        isNotified = null,
        isExpanded = null,
        isSticky = null
    )
}

private fun buildSampleNewLabel(name: String = "", color: String = ""): NewLabel {
    return NewLabel(
        parentId = null,
        name = name,
        type = LabelType.MessageLabel,
        color = color,
        isNotified = null,
        isExpanded = null,
        isSticky = null
    )
}
