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

package ch.protonmail.android.mailcontact.presentation.contacgroupform

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
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
fun ContactGroupFormScreen(
    actions: ContactGroupFormScreen.Actions,
    viewModel: ContactGroupFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ContactGroupFormViewModel.initialState).value

    Scaffold(
        topBar = {
            ContactGroupFormTopBar(
                actions = ContactGroupFormTopBar.Actions(
                    onClose = {
                        viewModel.submit(ContactGroupFormViewAction.OnCloseClick)
                    },
                    onSave = {
                        // Call view model with save view action here
                    }
                )
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactGroupFormState.Data -> {
                    // Display content here
                }
                is ContactGroupFormState.Loading -> {
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
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                hostState = snackbarHostErrorState
            )
        }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        actions.onClose()
    }
}

@Composable
fun ContactGroupFormTopBar(actions: ContactGroupFormTopBar.Actions) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { },
        navigationIcon = {
            IconButton(onClick = actions.onClose) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            ProtonTextButton(onClick = actions.onSave) {
                val textColor = ProtonTheme.colors.textAccent
                Text(
                    text = stringResource(id = R.string.contact_group_form_save),
                    color = textColor,
                    style = ProtonTheme.typography.defaultStrongNorm
                )
            }
        }
    )
}

object ContactGroupFormScreen {

    const val ContactGroupFormLabelIdKey = "contact_group_form_label_id"

    data class Actions(
        val onClose: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClose = {},
                exitWithErrorMessage = {}
            )
        }
    }
}

object ContactGroupFormTopBar {
    data class Actions(
        val onClose: () -> Unit,
        val onSave: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onClose = {},
                onSave = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactFormTopBarPreview() {
    ContactGroupFormTopBar(
        actions = ContactGroupFormTopBar.Actions.Empty
    )
}
